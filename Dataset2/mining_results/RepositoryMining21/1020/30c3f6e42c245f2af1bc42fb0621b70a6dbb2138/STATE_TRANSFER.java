// $Id: STATE_TRANSFER.java,v 1.26 2006/01/12 12:30:43 belaban Exp $

package org.jgroups.protocols.pbcast;

import org.jgroups.*;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.StateTransferInfo;
import org.jgroups.util.List;
import org.jgroups.util.Util;
import org.jgroups.util.Streamable;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;


/**
 * New STATE_TRANSFER protocol based on PBCAST. Compared to the one in ./protocols, it doesn't
 * need a QUEUE layer above it. A state request is sent to a chosen member (coordinator if
 * null). That member makes a copy D of its current digest and asks the application for a copy of
 * its current state S. Then the member returns both S and D to the requester. The requester
 * first sets its digest to D and then returns the state to the application.
 * @author Bela Ban
 */
public class STATE_TRANSFER extends Protocol {
    Address        local_addr=null;
    final Vector   members=new Vector();
    long           state_id=1;  // used to differentiate between state transfers (not currently used)
    final List     state_requesters=new List(); // requesters of state (usually just 1, could be more)
    Digest         digest=null;
    final HashMap  map=new HashMap(); // to store configuration information
    long           start, stop; // to measure state transfer time
    int            num_state_reqs=0;
    long           num_bytes_sent=0;
    double         avg_state_size=0;
    final static   String name="STATE_TRANSFER";


    /** All protocol names have to be unique ! */
    public String getName() {
        return name;
    }

    public int getNumberOfStateRequests() {return num_state_reqs;}
    public long getNumberOfStateBytesSent() {return num_bytes_sent;}
    public double getAverageStateSize() {return avg_state_size;}

    public Vector requiredDownServices() {
        Vector retval=new Vector();
        retval.addElement(new Integer(Event.GET_DIGEST_STATE));
        retval.addElement(new Integer(Event.SET_DIGEST));
        return retval;
    }

    public void resetStats() {
        super.resetStats();
        num_state_reqs=0;
        num_bytes_sent=0;
        avg_state_size=0;
    }


    public boolean setProperties(Properties props) {
        super.setProperties(props);

        if(props.size() > 0) {
            log.error("the following properties are not recognized: " + props);

            return false;
        }
        return true;
    }

    public void init() throws Exception {
        map.put("state_transfer", Boolean.TRUE);
        map.put("protocol_class", getClass().getName());
    }


    public void start() throws Exception {
        passUp(new Event(Event.CONFIG, map));
    }


    public void up(Event evt) {
        Message     msg;
        StateHeader hdr;

        switch(evt.getType()) {

        case Event.BECOME_SERVER:
            break;

        case Event.SET_LOCAL_ADDRESS:
            local_addr=(Address)evt.getArg();
            break;

        case Event.TMP_VIEW:
        case Event.VIEW_CHANGE:
            handleViewChange((View)evt.getArg());
            break;

        case Event.GET_DIGEST_STATE_OK:
            synchronized(state_requesters) {
                if(digest != null) {
                    if(warn)
                        log.warn("GET_DIGEST_STATE_OK: existing digest is not null, overwriting it !");
                }
                digest=(Digest)evt.getArg();
                if(log.isDebugEnabled())
                    log.debug("GET_DIGEST_STATE_OK: digest is " + digest + "\npassUp(GET_APPLSTATE)");
                passUp(new Event(Event.GET_APPLSTATE));
            }
            return;

        case Event.MSG:
            msg=(Message)evt.getArg();
            if(!(msg.getHeader(name) instanceof StateHeader))
                break;

            hdr=(StateHeader)msg.removeHeader(name);
            switch(hdr.type) {
            case StateHeader.STATE_REQ:
                handleStateReq(hdr.sender);
                break;
            case StateHeader.STATE_RSP:
                handleStateRsp(hdr.sender, hdr.my_digest, msg.getBuffer());
                break;
            default:
                if(log.isErrorEnabled()) log.error("type " + hdr.type + " not known in StateHeader");
                break;
            }
            return;
        }
        passUp(evt);
    }



    public void down(Event evt) {
        byte[] state;
        Address target, requester;
        StateTransferInfo info;
        StateHeader hdr;
        Message state_req, state_rsp;

        switch(evt.getType()) {

            case Event.TMP_VIEW:
            case Event.VIEW_CHANGE:
                handleViewChange((View)evt.getArg());
                break;

            // generated by JChannel.getState(). currently, getting the state from more than 1 mbr is not implemented
            case Event.GET_STATE:
                info=(StateTransferInfo)evt.getArg();
                if(info.type != StateTransferInfo.GET_FROM_SINGLE) {
                    if(warn) log.warn("[GET_STATE] (info=" + info + "): getting the state from " +
                            "all members is not currently supported by pbcast.STATE_TRANSFER, will use " +
                            "coordinator to fetch state instead");
                }
                if(info.target == null) {
                    target=determineCoordinator();
                }
                else {
                    target=info.target;
                    if(target.equals(local_addr)) {
                        if(log.isErrorEnabled()) log.error("GET_STATE: cannot fetch state from myself !");
                        target=null;
                    }
                }
                if(target == null) {
                    if(log.isDebugEnabled()) log.debug("GET_STATE: first member (no state)");
                    passUp(new Event(Event.GET_STATE_OK, null));
                }
                else {
                    state_req=new Message(target, null, null);
                    state_req.putHeader(name, new StateHeader(StateHeader.STATE_REQ, local_addr, state_id++, null));
                    if(log.isDebugEnabled()) log.debug("GET_STATE: asking " + target + " for state");

                    // suspend sending and handling of mesage garbage collection gossip messages,
                    // fixes bugs #943480 and #938584). Wake up when state has been received
                    if(log.isDebugEnabled())
                        log.debug("passing down a SUSPEND_STABLE event");
                    passDown(new Event(Event.SUSPEND_STABLE, new Long(info.timeout)));

                    start=System.currentTimeMillis();
                    passDown(new Event(Event.MSG, state_req));
                }
                return;                 // don't pass down any further !

            case Event.GET_APPLSTATE_OK:
                state=(byte[])evt.getArg();
                synchronized(state_requesters) {
                    if(state_requesters.size() == 0) {
                        if(warn)
                            log.warn("GET_APPLSTATE_OK: received application state, but there are no requesters !");
                        return;
                    }
                    if(digest == null)
                        if(warn) log.warn("GET_APPLSTATE_OK: received application state, " +
                                "but there is no digest !");
                    else
                        digest=digest.copy();
                    if(stats) {
                        num_state_reqs++;
                        if(state != null)
                            num_bytes_sent+=state.length;
                        avg_state_size=num_bytes_sent / num_state_reqs;
                    }
                    for(Enumeration e=state_requesters.elements(); e.hasMoreElements();) {
                        requester=(Address)e.nextElement();
                        state_rsp=new Message(requester, null, state); // put the state into state_rsp.buffer
                        hdr=new StateHeader(StateHeader.STATE_RSP, local_addr, 0, digest);
                        state_rsp.putHeader(name, hdr);
                        if(trace)
                            log.trace("sending state to " + requester + " (" + state.length + " bytes)");
                        passDown(new Event(Event.MSG, state_rsp));
                    }
                    digest=null;
                    state_requesters.removeAll();
                }
                return;                 // don't pass down any further !
        }

        passDown(evt);              // pass on to the layer below us
    }









    /* --------------------------- Private Methods -------------------------------- */


    /** Return the first element of members which is not me. Otherwise return null. */
    private Address determineCoordinator() {
        Address ret=null;
        synchronized(members) {
            if(members != null && members.size() > 1) {
                for(int i=0; i < members.size(); i++)
                    if(!local_addr.equals(members.elementAt(i)))
                        return (Address)members.elementAt(i);
            }
        }
        return ret;
    }


    private void handleViewChange(View v) {
        Vector new_members=v.getMembers();
        synchronized(members) {
            members.clear();
            members.addAll(new_members);
        }
    }

    /**
     * If a state transfer is in progress, we don't need to send a GET_APPLSTATE event to the application, but
     * instead we just add the sender to the requester list so it will receive the same state when done. If not,
     * we add the sender to the requester list and send a GET_APPLSTATE event up.
     */
    private void handleStateReq(Object sender) {
        if(sender == null) {
            if(log.isErrorEnabled()) log.error("sender is null !");
            return;
        }

        synchronized(state_requesters) {
            if(state_requesters.size() > 0) {  // state transfer is in progress, digest was requested
                state_requesters.add(sender);
            }
            else {
                state_requesters.add(sender);
                digest=null;
                if(log.isDebugEnabled()) log.debug("passing down GET_DIGEST_STATE");
                passDown(new Event(Event.GET_DIGEST_STATE));
            }
        }
    }


    /** Set the digest and the send the state up to the application */
    void handleStateRsp(Object sender, Digest digest, byte[] state) {
        if(digest == null) {
            if(warn)
                log.warn("digest received from " + sender + " is null, skipping setting digest !");
        }
        else
            passDown(new Event(Event.SET_DIGEST, digest)); // set the digest (e.g. in NAKACK)
        stop=System.currentTimeMillis();

        // resume sending and handling of mesage garbage collection gossip messages,
        // fixes bugs #943480 and #938584). Wakes up a previously suspended message garbage
        // collection protocol (e.g. STABLE)
        if(log.isDebugEnabled())
            log.debug("passing down a RESUME_STABLE event");
        passDown(new Event(Event.RESUME_STABLE));

        if(state == null) {
            if(warn)
                log.warn("state received from " + sender + " is null, will return null state to application");
        }
        else
            log.debug("received state, size=" + state.length + " bytes. Time=" + (stop-start) + " milliseconds");
        passUp(new Event(Event.GET_STATE_OK, state));
    }


    /* ------------------------ End of Private Methods ------------------------------ */



    /**
     * Wraps data for a state request/response. Note that for a state response the actual state will <em>not</em
     * be stored in the header itself, but in the message's buffer.
     *
     */
    public static class StateHeader extends Header implements Streamable {
        public static final byte STATE_REQ=1;
        public static final byte STATE_RSP=2;


        long id=0;          // state transfer ID (to separate multiple state transfers at the same time)
        byte type=0;
        Address sender=null;   // sender of state STATE_REQ or STATE_RSP
        Digest my_digest=null;   // digest of sender (if type is STATE_RSP)


        public StateHeader() {
        } // for externalization


        public StateHeader(byte type, Address sender, long id, Digest digest) {
            this.type=type;
            this.sender=sender;
            this.id=id;
            this.my_digest=digest;
        }

        public int getType() {
            return type;
        }

        public Digest getDigest() {
            return my_digest;
        }


        public boolean equals(Object o) {
            StateHeader other;

            if(sender != null && o != null) {
                if(!(o instanceof StateHeader))
                    return false;
                other=(StateHeader)o;
                return sender.equals(other.sender) && id == other.id;
            }
            return false;
        }


        public int hashCode() {
            if(sender != null)
                return sender.hashCode() + (int)id;
            else
                return (int)id;
        }


        public String toString() {
            StringBuffer sb=new StringBuffer();
            sb.append("[StateHeader: type=" + type2Str(type));
            if(sender != null) sb.append(", sender=" + sender + " id=#" + id);
            if(my_digest != null) sb.append(", digest=" + my_digest);
            return sb.toString();
        }


        static String type2Str(int t) {
            switch(t) {
                case STATE_REQ:
                    return "STATE_REQ";
                case STATE_RSP:
                    return "STATE_RSP";
                default:
                    return "<unknown>";
            }
        }


        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(sender);
            out.writeLong(id);
            out.writeByte(type);
            out.writeObject(my_digest);
        }


        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            sender=(Address)in.readObject();
            id=in.readLong();
            type=in.readByte();
            my_digest=(Digest)in.readObject();
        }



        public void writeTo(DataOutputStream out) throws IOException {
            out.writeByte(type);
            out.writeLong(id);
            Util.writeAddress(sender, out);
            Util.writeStreamable(my_digest, out);
        }

        public void readFrom(DataInputStream in) throws IOException, IllegalAccessException, InstantiationException {
            type=in.readByte();
            id=in.readLong();
            sender=Util.readAddress(in);
            my_digest=(Digest)Util.readStreamable(Digest.class, in);
        }

        public long size() {
            long retval=Global.LONG_SIZE + Global.BYTE_SIZE; // id and type

            retval+=Util.size(sender);

            retval+=Global.BYTE_SIZE; // presence byte for my_digest
            if(my_digest != null)
                retval+=my_digest.serializedSize();

            return retval;
        }

    }


}
