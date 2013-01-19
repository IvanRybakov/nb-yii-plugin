/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.nbphpcouncil.modules.php.yii.editor;

import javax.swing.text.Document;
import org.nbphpcouncil.modules.php.yii.util.YiiUtils;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProvider;
import org.netbeans.modules.csl.api.UiUtils;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.openide.filesystems.FileObject;

/**
 *
 * @author junichi11
 */
@MimeRegistration(mimeType = "text/x-php5", service = HyperlinkProvider.class)
public class YiiGoToViewHyperlinkProvider implements HyperlinkProvider {

    private static final int DEFAULT_OFFSET = 0;
    private FileObject view = null;
    private String target = ""; // NOI18N
    private int targetStart;
    private int targetEnd;

    @Override
    public boolean isHyperlinkPoint(Document doc, int offset) {
        return verifyState(doc, offset);
    }

    @Override
    public int[] getHyperlinkSpan(Document doc, int offset) {
        if (view != null) {
            return new int[]{targetStart, targetEnd};
        }
        return null;
    }

    @Override
    public void performClickAction(Document doc, int offset) {
        // Open view file
        if (view != null) {
            UiUtils.open(view, DEFAULT_OFFSET);
        }
    }

    /**
     * Verify whether caret position is on view file name string.
     *
     * @param doc
     * @param offset
     * @return true if view file exsits, otherwise false.
     */
    private boolean verifyState(Document doc, int offset) {
        // get FileObject
        Source source = Source.create(doc);
        FileObject targetFile = source.getFileObject();
        if (!YiiUtils.isController(targetFile)) {
            return false;
        }

        TokenHierarchy hierarchy = TokenHierarchy.get(doc);
        TokenSequence<PHPTokenId> ts = hierarchy.tokenSequence(PHPTokenId.language());
        if (ts == null) {
            return false;
        }
        ts.move(offset);
        ts.moveNext();
        int newOffset = ts.offset();

        Token<PHPTokenId> token = ts.token();
        setTarget(token);

        if (target.isEmpty() || !isRenderMethod(ts)) {
            return false;
        }
        // set view file
        view = YiiUtils.getView(targetFile, target);
        if (view != null) {
            targetStart = newOffset + 1;
            targetEnd = targetStart + target.length();
            return true;
        }

        return false;
    }

    /**
     * Verify whether method name is "render".
     *
     * @param ts TokenSequence
     * @return true if render method, otherwise false
     */
    private boolean isRenderMethod(TokenSequence<PHPTokenId> ts) {
        ts.movePrevious();
        ts.movePrevious();
        Token<PHPTokenId> render = ts.token();
        if (render.text().toString().equals("render")) { // NOI18N
            return true;
        }
        return false;
    }

    /**
     * Set target string. If token is string on carret position, set stirng to
     * target, otherwise set empty string to it.
     *
     * @param token
     */
    private void setTarget(Token<PHPTokenId> token) {
        target = token.text().toString();
        PHPTokenId id = token.id();
        if (id == PHPTokenId.PHP_CONSTANT_ENCAPSED_STRING) {
            if (target.length() > 2) {
                target = target.substring(1, target.length() - 1);
            } else {
                target = ""; // NOI18N
            }
        } else {
            target = ""; // NOI18N
        }
    }
}
