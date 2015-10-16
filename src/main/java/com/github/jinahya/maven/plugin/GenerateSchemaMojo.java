/*
 * Copyright 2015 Jin Kwon &lt;jinahya_at_gmail.com&gt;.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.github.jinahya.maven.plugin;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;


/**
 *
 * @author Jin Kwon &lt;jinahya_at_gmail.com&gt;
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
      name = "generate-schema",
      requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
      requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GenerateSchemaMojo extends AbstractMojo {


    public void generate(final JAXBContext context)
        throws MojoExecutionException, MojoFailureException {

        getLog().info("generate(" + context + ")");

        try {
            context.generateSchema(new SchemaOutputResolver() {

                @Override
                public Result createOutput(final String namespaceUri,
                                           String suggestedFileName)
                    throws IOException {

                    getLog().info("namespaceUri: " + namespaceUri);
                    getLog().info("suggestedFileName: " + suggestedFileName);
                    if (XMLConstants.NULL_NS_URI.equals(namespaceUri)) {
                        suggestedFileName = "schema";
                    }

                    final File f = new File(outputDirectory, suggestedFileName);
                    final StreamResult result = new StreamResult(f);

                    return result;
                }

            });
        } catch (final IOException ioe) {
            throw new MojoFailureException(
                "failed to write schema to file", ioe);
        }
    }


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("classesToBeBound: " + Arrays.toString(classesToBeBound));
        getLog().info("contextPath: " + contextPath);

        getLog().info("project.artifactId: " + project.getArtifactId());

        final Set<URL> urls = new HashSet<URL>();
        for (String compileClasspathElement : compileClasspathElements) {
            try {
                urls.add(new File(compileClasspathElement).toURI().toURL());
            } catch (final MalformedURLException murle) {
            }
        }
        final ClassLoader classLoader = URLClassLoader.newInstance(
            urls.toArray(new URL[urls.size()]),
            Thread.currentThread().getContextClassLoader());
//        Thread.currentThread().setContextClassLoader(contextClassLoader);

        if (!outputDirectory.isDirectory()) {
            outputDirectory.mkdirs();
        }

        if (classesToBeBound != null && classesToBeBound.length > 0) {
            final Class<?>[] classes = new Class<?>[classesToBeBound.length];
            for (int i = 0; i < classesToBeBound.length; i++) {
                try {
                    classes[i] = Class.forName(classesToBeBound[i]);
                } catch (final ClassNotFoundException cnfe) {
                    throw new MojoFailureException(
                        "failed to load class: " + classesToBeBound[i]);
                }
            }
            try {
                generate(JAXBContext.newInstance(classes));
            } catch (final JAXBException jaxbe) {
                throw new MojoFailureException(
                    "failed to create JAXBContext with classesToBeBound("
                    + Arrays.toString(classesToBeBound) + ")", jaxbe);
            }
            return;
        }

        if (contextPath != null) {
            try {
                generate(JAXBContext.newInstance(contextPath, classLoader));
            } catch (final JAXBException jaxbe) {
                throw new MojoFailureException(
                    "failed to create JAXBContext with contextPath("
                    + contextPath + ")", jaxbe);
            }
            final Resource resource = new Resource();
            resource.setDirectory("target/generated-resources/jaxb-context");
            //resource.setDirectory(outputDirectory.getPath());
            //project.getBuild().getResources().add(resource);
            project.addResource(resource);
            getLog().info("project.resources: " + project.getResources());

            return;
        }

        if (true) {
            return;
        }

        throw new MojoFailureException(
            "either `classesToBeBound` or `contextPath` should be defined");
    }


    @Parameter(defaultValue = "${project}",
               //readonly = true,
               required = true)
    private MavenProject project;


    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;


    @Parameter(property = "project.compileClasspathElements",
               required = true, readonly = true)
    private List<String> compileClasspathElements;


    @Parameter(property = "jaxb.context.classes.to.be.bound")
    private String[] classesToBeBound;


    @Parameter(property = "jaxb.context.context.path")
    private String contextPath;


    @Parameter(defaultValue = "${project.build.directory}/generated-resources/jaxb-context",
               property = "jaxb.context.output.directory", required = true)
    private File outputDirectory;


    @Parameter(property = "jaxb.context.file.name", required = true)
    private String fileName;


}

