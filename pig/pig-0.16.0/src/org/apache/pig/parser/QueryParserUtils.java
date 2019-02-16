/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pig.parser;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.pig.FuncSpec;
import org.apache.pig.PigConfiguration;
import org.apache.pig.StoreFuncInterface;
import org.apache.pig.backend.datastorage.ContainerDescriptor;
import org.apache.pig.backend.datastorage.DataStorage;
import org.apache.pig.backend.datastorage.ElementDescriptor;
import org.apache.pig.backend.hadoop.datastorage.ConfigurationUtil;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.MRConfiguration;
import org.apache.pig.backend.hadoop.executionengine.shims.HadoopShims;
import org.apache.pig.builtin.PigStorage;
import org.apache.pig.impl.PigContext;
import org.apache.pig.impl.io.FileSpec;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.newplan.Operator;
import org.apache.pig.newplan.logical.relational.LOStore;
import org.apache.pig.newplan.logical.relational.LogicalPlan;
import org.apache.pig.tools.pigstats.ScriptState;

public class QueryParserUtils {

    public static String removeQuotes(String str) {
        if (str.startsWith("\u005c'") && str.endsWith("\u005c'"))
            return str.substring(1, str.length() - 1);
        else
            return str;
    }

    public static void attachStorePlan(String scope, LogicalPlan lp, String fileName, String func,
            Operator input, String alias, PigContext pigContext) throws FrontendException {
        func = func == null ? pigContext.getProperties().getProperty(PigConfiguration.PIG_DEFAULT_STORE_FUNC, PigStorage.class.getName()) : func;

        FuncSpec funcSpec = new FuncSpec( func );
        StoreFuncInterface stoFunc = (StoreFuncInterface)PigContext.instantiateFuncFromSpec( funcSpec );

        fileName = removeQuotes( fileName );
        FileSpec fileSpec = new FileSpec( fileName, funcSpec );
        String sig = alias + "_" + LogicalPlanBuilder.newOperatorKey(scope);
        stoFunc.setStoreFuncUDFContextSignature(sig);
        LOStore store = new LOStore(lp, fileSpec, stoFunc, sig);
        store.setAlias(alias);

        try {
            stoFunc.relToAbsPathForStoreLocation( fileName, getCurrentDir( pigContext ) );
        } catch (IOException ioe) {
            FrontendException e = new FrontendException(  ioe.getMessage(), ioe );
            throw e;
        }

        lp.add( store );
        lp.connect( input, store );
    }

    static Path getCurrentDir(PigContext pigContext) throws IOException {
        DataStorage dfs = pigContext.getDfs();
        ContainerDescriptor desc = dfs.getActiveContainer();
        ElementDescriptor el = dfs.asElement(desc);
        return new Path(el.toString());
    }

    static void setHdfsServers(String absolutePath, PigContext pigContext) throws URISyntaxException {
        // Get native host
        String defaultFS = (String)pigContext.getProperties().get("fs.default.name");
        if (defaultFS==null)
            defaultFS = (String)pigContext.getProperties().get("fs.defaultFS");

        URI defaultFSURI = new URI(defaultFS);

        Configuration conf = new Configuration(true);
        ConfigurationUtil.mergeConf(conf, ConfigurationUtil.toConfiguration(pigContext.getProperties()));
        Set<String> remoteHosts = getRemoteHosts(absolutePath, defaultFSURI, conf);

        String hdfsServersString = (String)pigContext.getProperties().get(MRConfiguration.JOB_HDFS_SERVERS);
        if (hdfsServersString == null) hdfsServersString = "";
        String hdfsServers[] = hdfsServersString.split(",");

        for (String remoteHost : remoteHosts) {
            boolean existing = false;
            for (String hdfsServer : hdfsServers) {
                if (hdfsServer.equals(remoteHost)) {
                    existing = true;
                }
            }
            if (!existing) {
                if (!hdfsServersString.isEmpty()) {
                    hdfsServersString = hdfsServersString + ",";
                }
                hdfsServersString = hdfsServersString + remoteHost;
            }
        }

        if (!hdfsServersString.isEmpty()) {
            pigContext.getProperties().setProperty(MRConfiguration.JOB_HDFS_SERVERS, hdfsServersString);
        }
    }

    static Set<String> getRemoteHosts(String absolutePath, URI defaultFSURI, Configuration conf) {
        String defaultHost = defaultFSURI.getHost() ==  null ? "" : defaultFSURI.getHost().toLowerCase();
        String defaultScheme = defaultFSURI.getScheme() == null ? "" : defaultFSURI.getScheme().toLowerCase();

        Set<String> result = new HashSet<String>();
        String[] fnames = absolutePath.split(",");
        for (String fname : fnames) {
            // remove leading/trailing whitespace(s)
            Path path = new Path(fname.trim());
            URI uri = path.toUri();
            if (uri.isAbsolute()) { // If it has scheme
                String thisHost = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
                String scheme = uri.getScheme().toLowerCase();
                // If host and scheme are same continue
                if (scheme.equals(defaultScheme) && (thisHost.equals(defaultHost) || thisHost.isEmpty())) {
                    continue;
                }
                String authority = uri.getAuthority() == null ? "" : uri.getAuthority()
                        .toLowerCase();
                if (scheme.equals("har")) {
                    String[] parts = authority.split("-", 2);
                    scheme = parts[0];
                    if (parts.length < 2) {
                        authority = "";
                    } else {
                        authority = parts[1];
                    }
                    if (scheme.isEmpty() || (scheme.equals(defaultScheme) &&
                            authority.equals(defaultFSURI.getAuthority()))) {
                        continue;
                    }
                } else if (!HadoopShims.hasFileSystemImpl(path, conf)) {
                    continue;
                }
                result.add(scheme + "://" + authority);
            }
        }
        return result;
     }

     static String constructFileNameSignature(String fileName, FuncSpec funcSpec) {
         return fileName + "_" + funcSpec.toString();
     }

    static String generateErrorHeader(RecognitionException ex, String filename) {
        return new SourceLocation( filename, ex.line, ex.charPositionInLine ).toString();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static void replaceNodeWithNodeList(Tree oldNode, CommonTree newTree,
            String fileName) {
        int idx = oldNode.getChildIndex();

        CommonTree parent = (CommonTree) oldNode.getParent();
        int count = parent.getChildCount();

        List childList = new ArrayList(parent.getChildren());
        List macroList = newTree.getChildren();

        while (parent.getChildCount() > 0) {
            parent.deleteChild(0);
        }

        for (int i = 0; i < count; i++) {
            if (i == idx) {
                // add only there is something to add
                if (macroList != null) {
                    parent.addChildren(macroList);
                }
            } else {
                parent.addChild((Tree) childList.get(i));
            }
        }
    }

     static File getFileFromImportSearchPath(String scriptPath) {
        File f = new File(scriptPath);
        if (f.exists() || f.isAbsolute() || scriptPath.startsWith("./")
                || scriptPath.startsWith("../")) {
            return f;
        }

        ScriptState state = ScriptState.get();
        if (state != null && state.getPigContext() != null) {
            String srchPath = state.getPigContext().getProperties()
                    .getProperty("pig.import.search.path");
            if (srchPath != null) {
                String[] paths = srchPath.split(",");
                for (String path : paths) {
                    File f1 = new File(path + File.separator + scriptPath);
                    if (f1.exists()) {
                        return f1;
                    }
                }
            }
        }

        return null;
    }

    static QueryParser createParser(CommonTokenStream tokens) {
        return createParser(tokens, 0);
    }

    static QueryParser createParser(CommonTokenStream tokens, int lineOffset) {
        QueryParser parser = new QueryParser(tokens);
        PigParserNodeAdaptor adaptor = new PigParserNodeAdaptor(
                tokens.getSourceName(), lineOffset);
        parser.setTreeAdaptor(adaptor);
        return parser;
    }
}
