/*
 * The MIT License
 *
 * Copyright 2013 Oleg Nenashev, Synopsys Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.synopsys.arc.jenkins.plugins.ownership.util;

import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import org.jenkinsci.plugins.ownership.util.mail.MailFormatter;
import hudson.model.User;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;

/**
 * Provides basic operations for ownership helpers.
 * @param <TObjectType> An item type, which is supported by the class.
 * @since 0.0.4
 * @author Oleg Nenashev
 */
public abstract class AbstractOwnershipHelper<TObjectType>  
    implements IOwnershipHelper<TObjectType> {
    
    /**An empty collection of users*/
    protected final static Collection<User> EMPTY_USERS_COLLECTION = new ArrayList<User>(0);
    
    
    @Override
    public final @Nonnull String getDisplayName(@CheckForNull User usr) {
        return UserStringFormatter.format(usr);
    } 
    
    @Override
    public final @CheckForNull String getOwnerEmail(@Nonnull TObjectType item) {
        OwnershipDescription descr = getOwnershipDescription(item);
        return  UserStringFormatter.formatEmail(descr.getPrimaryOwner());      
    }
      
    @Override
    public final @Nonnull String getOwnerLongString(@Nonnull TObjectType item) {
        OwnershipDescription descr = getOwnershipDescription(item);     
        return descr.isOwnershipEnabled() 
                ? UserStringFormatter.format(descr.getPrimaryOwner()) 
                : UserStringFormatter.UNKNOWN_USER_STRING;
    }
       
    @Override
    public final @Nonnull  String getOwner(@Nonnull TObjectType item) {
        OwnershipDescription descr = getOwnershipDescription(item);
        return (descr.isOwnershipEnabled()) 
                ? UserStringFormatter.formatShort(descr.getPrimaryOwnerId())
                : UserStringFormatter.UNKNOWN_USER_STRING;
    }    
    
    @Override
    public final boolean isOwnerExists(@Nonnull TObjectType item) {
        OwnershipDescription descr = getOwnershipDescription(item);
        return descr.isOwnershipEnabled() ? descr.hasPrimaryOwner() : false;
    }

    @Override
    public Collection<User> getPossibleOwners(TObjectType item) {
        return EMPTY_USERS_COLLECTION;
    }
}
