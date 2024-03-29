/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.gen;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.gen.xml.CgenExtension;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;

public class ClassGenerationAction implements Serializable, XMLSerializable {
	static final String TEMPLATES_DIR_NAME = "templates/v4_1/";

	public static final String SINGLE_CLASS_TEMPLATE = TEMPLATES_DIR_NAME + "singleclass.vm";
	public static final String SUBCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "subclass.vm";
	public static final String SUPERCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "superclass.vm";

	public static final String EMBEDDABLE_SINGLE_CLASS_TEMPLATE = TEMPLATES_DIR_NAME + "embeddable-singleclass.vm";
	public static final String EMBEDDABLE_SUBCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "embeddable-subclass.vm";
	public static final String EMBEDDABLE_SUPERCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "embeddable-superclass.vm";

	public static final String DATAMAP_SINGLE_CLASS_TEMPLATE = TEMPLATES_DIR_NAME + "datamap-singleclass.vm";
	public static final String DATAMAP_SUBCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "datamap-subclass.vm";
	public static final String DATAMAP_SUPERCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "datamap-superclass.vm";

	public static final String SUPERCLASS_PREFIX = "_";
	private static final String WILDCARD = "*";

	protected Collection<Artifact> artifacts;

	protected Collection<String> entityArtifacts;
	protected Collection<String> embeddableArtifacts;

	protected String superPkg;
	protected DataMap dataMap;

	protected ArtifactsGenerationMode artifactsGenerationMode;
	protected boolean makePairs;

	protected Logger logger;
	protected File destDir;
	protected boolean overwrite;
	protected boolean usePkgPath;

	protected String template;
	protected String superTemplate;
	protected String embeddableTemplate;
	protected String embeddableSuperTemplate;
	protected String queryTemplate;
	protected String querySuperTemplate;
	protected long timestamp;
	protected String outputPattern;
	protected String encoding;
	protected boolean createPropertyNames;
	protected boolean force; // force run generator

	/**
	 * @since 4.1
	 */
	protected boolean createPKProperties;

	// runtime ivars
	protected VelocityContext context;
	protected Map<String, Template> templateCache;

	public ClassGenerationAction() {
		this.outputPattern = "*.java";
		this.timestamp = 0L;
		this.usePkgPath = true;
		this.makePairs = true;
		this.context = new VelocityContext();
		this.templateCache = new HashMap<>(5);

		this.template = SUBCLASS_TEMPLATE;
		this.superTemplate = SUPERCLASS_TEMPLATE;

		this.embeddableTemplate = EMBEDDABLE_SUBCLASS_TEMPLATE;
		this.embeddableSuperTemplate = EMBEDDABLE_SUPERCLASS_TEMPLATE;

		this.queryTemplate = DATAMAP_SUBCLASS_TEMPLATE;
		this.querySuperTemplate = DATAMAP_SUPERCLASS_TEMPLATE;

		this.artifactsGenerationMode = ArtifactsGenerationMode.ENTITY;

		this.artifacts = new ArrayList<>();
		this.entityArtifacts = new ArrayList<>();
		this.embeddableArtifacts = new ArrayList<>();
	}

	protected String defaultTemplateName(TemplateType type) {
		switch (type) {
		case ENTITY_SINGLE_CLASS:
			return ClassGenerationAction.SINGLE_CLASS_TEMPLATE;
		case ENTITY_SUBCLASS:
			return ClassGenerationAction.SUBCLASS_TEMPLATE;
		case ENTITY_SUPERCLASS:
			return ClassGenerationAction.SUPERCLASS_TEMPLATE;
		case EMBEDDABLE_SUBCLASS:
			return ClassGenerationAction.EMBEDDABLE_SUBCLASS_TEMPLATE;
		case EMBEDDABLE_SUPERCLASS:
			return ClassGenerationAction.EMBEDDABLE_SUPERCLASS_TEMPLATE;
		case EMBEDDABLE_SINGLE_CLASS:
			return ClassGenerationAction.EMBEDDABLE_SINGLE_CLASS_TEMPLATE;
		case DATAMAP_SINGLE_CLASS:
			return ClassGenerationAction.DATAMAP_SINGLE_CLASS_TEMPLATE;
		case DATAMAP_SUPERCLASS:
			return ClassGenerationAction.DATAMAP_SUPERCLASS_TEMPLATE;
		case DATAMAP_SUBCLASS:
			return ClassGenerationAction.DATAMAP_SUBCLASS_TEMPLATE;
		default:
			throw new IllegalArgumentException("Invalid template type: " + type);
		}
	}

	protected String customTemplateName(TemplateType type) {
		switch (type) {
		case ENTITY_SINGLE_CLASS:
			return template;
		case ENTITY_SUBCLASS:
			return template;
		case ENTITY_SUPERCLASS:
			return superTemplate;
		case EMBEDDABLE_SINGLE_CLASS:
			return embeddableTemplate;
		case EMBEDDABLE_SUBCLASS:
			return embeddableTemplate;
		case EMBEDDABLE_SUPERCLASS:
			return embeddableSuperTemplate;
		case DATAMAP_SINGLE_CLASS:
			return queryTemplate;
		case DATAMAP_SUPERCLASS:
			return querySuperTemplate;
		case DATAMAP_SUBCLASS:
			return queryTemplate;
		default:
			throw new IllegalArgumentException("Invalid template type: " + type);
		}
	}

	/**
	 * Returns a String used to prefix class name to create a generated
	 * superclass. Default value is "_".
	 */
	private String getSuperclassPrefix() {
		return ClassGenerationAction.SUPERCLASS_PREFIX;
	}

	/**
	 * VelocityContext initialization method called once per artifact.
	 */
	private void resetContextForArtifact(Artifact artifact) {
		StringUtils stringUtils = StringUtils.getInstance();

		String qualifiedClassName = artifact.getQualifiedClassName();
		String packageName = stringUtils.stripClass(qualifiedClassName);
		String className = stringUtils.stripPackageName(qualifiedClassName);

		String qualifiedBaseClassName = artifact.getQualifiedBaseClassName();
		String basePackageName = stringUtils.stripClass(qualifiedBaseClassName);
		String baseClassName = stringUtils.stripPackageName(qualifiedBaseClassName);

		String superClassName = getSuperclassPrefix() + stringUtils.stripPackageName(qualifiedClassName);

		String superPackageName = this.superPkg;
		if (superPackageName == null) {
			superPackageName = packageName + ".auto";
		}

		context.put(Artifact.BASE_CLASS_KEY, baseClassName);
		context.put(Artifact.BASE_PACKAGE_KEY, basePackageName);

		context.put(Artifact.SUB_CLASS_KEY, className);
		context.put(Artifact.SUB_PACKAGE_KEY, packageName);

		context.put(Artifact.SUPER_CLASS_KEY, superClassName);
		context.put(Artifact.SUPER_PACKAGE_KEY, superPackageName);

		context.put(Artifact.OBJECT_KEY, artifact.getObject());
		context.put(Artifact.STRING_UTILS_KEY, stringUtils);

		context.put(Artifact.CREATE_PROPERTY_NAMES, createPropertyNames);
		context.put(Artifact.CREATE_PK_PROPERTIES, createPKProperties);
	}

	/**
	 * VelocityContext initialization method called once per each artifact and
	 * template type combination.
	 */
	void resetContextForArtifactTemplate(Artifact artifact, TemplateType templateType) {
		context.put(Artifact.IMPORT_UTILS_KEY, new ImportUtils());
		artifact.postInitContext(context);
	}

	public void prepareArtifacts() {
        resetArtifacts();
        addAllEntities();
        addAllEmbeddables();
        addQueries(dataMap.getQueryDescriptors());
    }

	/**
	 * Executes class generation once per each artifact.
	 */
	public void execute() throws Exception {

		validateAttributes();

		try {
			for (Artifact artifact : artifacts) {
				execute(artifact);
			}
		} finally {
			// must reset engine at the end of class generator run to avoid
			// memory
			// leaks and stale templates
			this.templateCache.clear();
		}
	}

	/**
	 * Executes class generation for a single artifact.
	 */
	protected void execute(Artifact artifact) throws Exception {

		resetContextForArtifact(artifact);

		ArtifactGenerationMode artifactMode = makePairs ? ArtifactGenerationMode.GENERATION_GAP
				: ArtifactGenerationMode.SINGLE_CLASS;

		TemplateType[] templateTypes = artifact.getTemplateTypes(artifactMode);
		for (TemplateType type : templateTypes) {

			try (Writer out = openWriter(type)) {
				if (out != null) {

					resetContextForArtifactTemplate(artifact, type);
					getTemplate(type).merge(context, out);
				}
			}
		}
	}

	private Template getTemplate(TemplateType type) {

		String templateName = customTemplateName(type);
		if (templateName == null) {
			templateName = defaultTemplateName(type);
		}

		// Velocity < 1.5 has some memory problems, so we will create a VelocityEngine every time,
		// and store templates in an internal cache, to avoid uncontrolled memory leaks...
		// Presumably 1.5 fixes it.

		Template template = templateCache.get(templateName);

		if (template == null) {

			Properties props = new Properties();

			props.put("resource.loader", "cayenne");
			props.put("cayenne.resource.loader.class", ClassGeneratorResourceLoader.class.getName());
			props.put("cayenne.resource.loader.cache", "false");

			VelocityEngine velocityEngine = new VelocityEngine();
			velocityEngine.init(props);

			template = velocityEngine.getTemplate(templateName);
			templateCache.put(templateName, template);
		}

		return template;
	}

	/**
	 * Validates the state of this class generator.
	 * Throws CayenneRuntimeException if it is in an inconsistent state.
	 * Called internally from "execute".
	 */
	private void validateAttributes() {
		if (destDir == null) {
			throw new CayenneRuntimeException("'destDir' attribute is missing.");
		}

		if (!destDir.isDirectory()) {
			throw new CayenneRuntimeException("'destDir' is not a directory.");
		}

		if (!destDir.canWrite()) {
			throw new CayenneRuntimeException("Do not have write permissions for %s", destDir);
		}
	}

	/**
	 * Sets the destDir.
	 */
	public void setDestDir(File destDir) {
		this.destDir = destDir;
	}

	/**
	 * Sets <code>overwrite</code> property.
	 */
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	/**
	 * Sets <code>makepairs</code> property.
	 */
	public void setMakePairs(boolean makePairs) {
		this.makePairs = makePairs;
	}

	/**
	 * Sets <code>template</code> property.
	 */
	public void setTemplate(String template) {
		this.template = template;
	}

	/**
	 * Sets <code>superTemplate</code> property.
	 */
	public void setSuperTemplate(String superTemplate) {
		this.superTemplate = superTemplate;
	}

	public void setQueryTemplate(String queryTemplate) {
		this.queryTemplate = queryTemplate;
	}

	public void setQuerySuperTemplate(String querySuperTemplate) {
		this.querySuperTemplate = querySuperTemplate;
	}

	/**
	 * Sets <code>usepkgpath</code> property.
	 */
	public void setUsePkgPath(boolean usePkgPath) {
		this.usePkgPath = usePkgPath;
	}

	/**
	 * Sets <code>outputPattern</code> property.
	 */
	public void setOutputPattern(String outputPattern) {
		this.outputPattern = outputPattern;
	}

	/**
	 * Sets <code>createPropertyNames</code> property.
	 */
	public void setCreatePropertyNames(boolean createPropertyNames) {
		this.createPropertyNames = createPropertyNames;
	}

	/**
	 * Opens a Writer to write generated output. Returned Writer is mapped to a
	 * filesystem file (although subclasses may override that). File location is
	 * determined from the current state of VelocityContext and the TemplateType
	 * passed as a parameter. Writer encoding is determined from the value of
	 * the "encoding" property.
	 */
	protected Writer openWriter(TemplateType templateType) throws Exception {

		File outFile = (templateType.isSuperclass()) ? fileForSuperclass() : fileForClass();
		if (outFile == null) {
			return null;
		}

		if (logger != null) {
			String label = templateType.isSuperclass() ? "superclass" : "class";
			logger.info("Generating " + label + " file: " + outFile.getCanonicalPath());
		}

		// return writer with specified encoding
		FileOutputStream out = new FileOutputStream(outFile);

		return (encoding != null) ? new OutputStreamWriter(out, encoding) : new OutputStreamWriter(out);
	}

	/**
	 * Returns a target file where a generated superclass must be saved. If null
	 * is returned, class shouldn't be generated.
	 */
	private File fileForSuperclass() throws Exception {

		String packageName = (String) context.get(Artifact.SUPER_PACKAGE_KEY);
		String className = (String) context.get(Artifact.SUPER_CLASS_KEY);

		String filename = StringUtils.getInstance().replaceWildcardInStringWithString(WILDCARD, outputPattern, className);
		File dest = new File(mkpath(destDir, packageName), filename);

		if (dest.exists() && !fileNeedUpdate(dest, superTemplate)) {
			return null;
		}

		return dest;
	}

	/**
	 * Returns a target file where a generated class must be saved. If null is
	 * returned, class shouldn't be generated.
	 */
	private File fileForClass() throws Exception {

		String packageName = (String) context.get(Artifact.SUB_PACKAGE_KEY);
		String className = (String) context.get(Artifact.SUB_CLASS_KEY);

		String filename = StringUtils.getInstance().replaceWildcardInStringWithString(WILDCARD, outputPattern, className);
		File dest = new File(mkpath(destDir, packageName), filename);

		if (dest.exists()) {
			// no overwrite of subclasses
			if (makePairs) {
				return null;
			}

			// skip if said so
			if (!overwrite) {
				return null;
			}

			if (!fileNeedUpdate(dest, template)) {
				return null;
			}
		}

		return dest;
	}

	/**
	 * Ignore if the destination is newer than the map
	 * (internal timestamp), i.e. has been generated after the map was
	 * last saved AND the template is older than the destination file
	 */
	protected boolean fileNeedUpdate(File dest, String templateFileName) {
		if(force) {
			return true;
		}

		if (isOld(dest)) {
            if (templateFileName == null) {
				return false;
            }

            File templateFile = new File(templateFileName);
			return templateFile.lastModified() >= dest.lastModified();
        }
		return true;
	}

	/**
	 * Is file modified after internal timestamp (usually equal to mtime of datamap file)
	 */
	protected boolean isOld(File file) {
		return file.lastModified() > timestamp;
	}

	/**
	 * Returns a File object corresponding to a directory where files that
	 * belong to <code>pkgName</code> package should reside. Creates any missing
	 * diectories below <code>dest</code>.
	 */
	private File mkpath(File dest, String pkgName) throws Exception {

		if (!usePkgPath || pkgName == null) {
			return dest;
		}

		String path = pkgName.replace('.', File.separatorChar);
		File fullPath = new File(dest, path);
		if (!fullPath.isDirectory() && !fullPath.mkdirs()) {
			throw new Exception("Error making path: " + fullPath);
		}

		return fullPath;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Sets file encoding. If set to null, default system encoding will be used.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Sets "superPkg" property value.
	 */
	public void setSuperPkg(String superPkg) {
		this.superPkg = superPkg;
	}

	/**
	 * @param dataMap The dataMap to set.
	 */
	public void setDataMap(DataMap dataMap) {
		this.dataMap = dataMap;
	}

	/**
	 * Adds entities to the internal entity list.
	 * @param entities collection
	 *
	 * @since 4.0 throws exception
	 */
	public void addEntities(Collection<ObjEntity> entities) {
		if (artifactsGenerationMode == ArtifactsGenerationMode.ENTITY
				|| artifactsGenerationMode == ArtifactsGenerationMode.ALL) {
			if (entities != null) {
				for (ObjEntity entity : entities) {
					artifacts.add(new EntityArtifact(entity));
				}
			}
		}
	}

	public void addEmbeddables(Collection<Embeddable> embeddables) {
		if (artifactsGenerationMode == ArtifactsGenerationMode.ENTITY
				|| artifactsGenerationMode == ArtifactsGenerationMode.ALL) {
			if (embeddables != null) {
				for (Embeddable embeddable : embeddables) {
					artifacts.add(new EmbeddableArtifact(embeddable));
				}
			}
		}
	}

	public void addQueries(Collection<QueryDescriptor> queries) {
		if (artifactsGenerationMode == ArtifactsGenerationMode.DATAMAP
				|| artifactsGenerationMode == ArtifactsGenerationMode.ALL) {

			// TODO: andrus 10.12.2010 - why not also check for empty query list??
			// Or create a better API for enabling DataMapArtifact
			if (queries != null) {
				artifacts.add(new DataMapArtifact(dataMap, queries));
			}
		}
	}

    private void addAllEntities() {
		if(artifactsGenerationMode == ArtifactsGenerationMode.ENTITY
				|| artifactsGenerationMode == ArtifactsGenerationMode.ALL) {
            entityArtifacts.forEach(val ->
                artifacts.add(new EntityArtifact(dataMap.getObjEntity(val))));
		}
	}

    private void addAllEmbeddables() {
		if(artifactsGenerationMode == ArtifactsGenerationMode.ENTITY
				|| artifactsGenerationMode == ArtifactsGenerationMode.ALL) {
		    embeddableArtifacts.forEach(val ->
                    artifacts.add(new EmbeddableArtifact(dataMap.getEmbeddable(val))));
		}
	}

    /**
     * @since 4.1
     */
	public void loadEntity(String name) {
		entityArtifacts.add(name);
	}

    /**
     * @since 4.1
     */
	public void loadEmbeddable(String name) {
		embeddableArtifacts.add(name);
	}

	/**
	 * Sets an optional shared VelocityContext. Useful with tools like VPP that
	 * can set custom values in the context, not known to Cayenne.
	 */
	public void setContext(VelocityContext context) {
		this.context = context;
	}

	/**
	 * Injects an optional logger that will be used to trace generated files at
	 * the info level.
	 */
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public void setEmbeddableTemplate(String embeddableTemplate) {
		this.embeddableTemplate = embeddableTemplate;
	}

	public void setEmbeddableSuperTemplate(String embeddableSuperTemplate) {
		this.embeddableSuperTemplate = embeddableSuperTemplate;
	}

	public void setArtifactsGenerationMode(String mode) {
		if (ArtifactsGenerationMode.ENTITY.getLabel().equalsIgnoreCase(mode)) {
			this.artifactsGenerationMode = ArtifactsGenerationMode.ENTITY;
		} else if (ArtifactsGenerationMode.DATAMAP.getLabel().equalsIgnoreCase(mode)) {
			this.artifactsGenerationMode = ArtifactsGenerationMode.DATAMAP;
		} else {
			this.artifactsGenerationMode = ArtifactsGenerationMode.ALL;
		}
	}

    /**
     * @since 4.1
     */
    public boolean isCreatePKProperties() {
        return createPKProperties;
    }

    /**
     * @since 4.1
     */
    public void setCreatePKProperties(boolean createPKProperties) {
        this.createPKProperties = createPKProperties;
    }

    private Collection<EntityArtifact> getEntityArtifacts() {
		resetArtifacts();
		addAllEntities();
		Collection<EntityArtifact> entityArtifacts = new ArrayList<>();
		for(Artifact artifact : artifacts){
			if(artifact instanceof EntityArtifact){
				entityArtifacts.add((EntityArtifact) artifact);
			}
		}
		return entityArtifacts;
	}

    private Collection<EmbeddableArtifact> getEmbeddableArtifacts() {
		resetArtifacts();
		addAllEmbeddables();
		Collection<EmbeddableArtifact> embeddableArtifacts = new ArrayList<>();
		for(Artifact artifact : artifacts){
			if(artifact instanceof EmbeddableArtifact){
				embeddableArtifacts.add((EmbeddableArtifact) artifact);
			}
		}
		return embeddableArtifacts;
	}

	public boolean isMakePairs() {
		return makePairs;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public boolean isUsePkgPath() {
		return usePkgPath;
	}

	public boolean isCreatePropertyNames() {
		return createPropertyNames;
	}

	public String getOutputPattern() {
		return outputPattern;
	}

	public String getSuperclassTemplate(){
		return superTemplate;
	}

	public DataMap getDataMap() {
		return dataMap;
	}

	public String getDir(){
		return destDir.getAbsolutePath();
	}

	public String getTemplate() {
		return template;
	}

	public String getSuperPkg(){
		return superPkg;
	}

	private void resetArtifacts(){
		this.artifacts = new ArrayList<>();
	}

	public Collection<String> getEntities() {
		return entityArtifacts;
	}

	public Collection<String> getEmbeddables() {
		return embeddableArtifacts;
	}

	public String getArtifactsGenerationMode(){
		return artifactsGenerationMode.getLabel();
	}

	public boolean isForce() {
		return force;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public String getEncoding() {
		return encoding;
	}

	public String getEmbeddableTemplate() {
		return embeddableTemplate;
	}

	public String getEmbeddableSuperTemplate() {
		return embeddableSuperTemplate;
	}

	public String getQueryTemplate() {
	    return queryTemplate;
    }

    public String getQuerySuperTemplate() {
        return querySuperTemplate;
    }

    public void resetCollections(){
		this.embeddableArtifacts = new ArrayList<>();
		this.entityArtifacts = new ArrayList<>();
	}

	@Override
	public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
		encoder.start("cgen")
				.attribute("xmlns", CgenExtension.NAMESPACE)
				.nested(this.getEntityArtifacts(), delegate)
				.nested(this.getEmbeddableArtifacts(), delegate)
				.simpleTag("outputDirectory", this.destDir.getAbsolutePath())
				.simpleTag("generationMode", this.artifactsGenerationMode.getLabel())
                .simpleTag("dataMapTemplate", this.queryTemplate)
                .simpleTag("dataMapSuperclassTemplate", this.querySuperTemplate)
				.simpleTag("subclassTemplate", this.template)
				.simpleTag("superclassTemplate", this.superTemplate)
				.simpleTag("embeddableTemplate", this.embeddableTemplate)
				.simpleTag("embeddableSuperclassTemplate", this.embeddableSuperTemplate)
				.simpleTag("outputPattern", this.outputPattern)
				.simpleTag("makePairs", Boolean.toString(this.makePairs))
				.simpleTag("usePkgPath", Boolean.toString(this.usePkgPath))
				.simpleTag("overwriteSubclasses", Boolean.toString(this.overwrite))
				.simpleTag("createPropertyNames", Boolean.toString(this.createPropertyNames))
				.simpleTag("superPkg", this.superPkg)
				.simpleTag("encoding", this.encoding)
				.end();
	}
}
