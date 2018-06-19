/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.lib.nbjavac.services;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javadoc.main.JavadocMemberEnter;

/**
 *
 * @author lahvac
 */
public class NBJavadocMemberEnter extends JavadocMemberEnter {

    public static void preRegister(Context context) {
        context.put(memberEnterKey, new Context.Factory<MemberEnter>() {
            public MemberEnter make(Context c) {
                return new NBJavadocMemberEnter(c);
            }
        });
    }

    private final CancelService cancelService;
    private final JavacTrees trees;

    public NBJavadocMemberEnter(Context context) {
        super(context);
        cancelService = CancelService.instance(context);
        trees = NBJavacTrees.instance(context);
    }

    @Override
    public void visitTopLevel(JCCompilationUnit tree) {
        cancelService.abortIfCanceled();
        super.visitTopLevel(tree);
    }

    @Override
    public void visitImport(JCImport tree) {
        cancelService.abortIfCanceled();
        super.visitImport(tree);
    }

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        cancelService.abortIfCanceled();
        JCBlock body = tree.body;
        try {
            super.visitMethodDef(tree);
        } finally {
            //reinstall body:
            tree.body = body;
        }
        if (trees instanceof NBJavacTrees && !env.enclClass.defs.contains(tree)) {
            TreePath path = trees.getPath(env.toplevel, env.enclClass);
            if (path != null) {
                ((NBJavacTrees)trees).addPathForElement(tree.sym, new TreePath(path, tree));
            }
        }
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        cancelService.abortIfCanceled();
        JCExpression init = tree.init;
        try {
            super.visitVarDef(tree);
        } finally {
            //reinstall init:
            tree.init = init;
            if (init != null) {
                tree.sym.flags_field |= Flags.HASINIT; //XXX: hack
            }
        }
    }

}
