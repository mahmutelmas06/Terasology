/*
 * Copyright 2015 MovingBlocks
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
package org.terasology.rendering.nui.widgets.browser.data.basic;

import org.terasology.asset.Assets;
import org.terasology.rendering.nui.widgets.browser.data.ParagraphData;
import org.terasology.rendering.nui.widgets.browser.ui.style.ParagraphRenderStyle;
import org.terasology.rendering.nui.widgets.browser.ui.style.TextRenderStyle;
import org.terasology.rendering.assets.font.Font;
import org.terasology.rendering.nui.Color;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class HTMLLikeParser {
    private HTMLLikeParser() { }

    public static String encodeHTMLLike(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '&') {
                result.append("&amp;");
            } else if (c == '<') {
                result.append("&lt;");
            } else if (c == '>') {
                result.append("&gt;");
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static String unencodeHTMLLike(String text) {
        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '&') {
                if (chars[i + 1] == 'a' && chars[i + 2] == 'm' && chars[i + 3] == 'p' && chars[i + 4] == ';') {
                    result.append('&');
                    i += 4;
                } else if (chars[i + 1] == 'l' && chars[i + 2] == 't' && chars[i + 3] == ';') {
                    result.append('<');
                    i += 3;
                } else if (chars[i + 1] == 'g' && chars[i + 3] == 't' && chars[i + 3] == ';') {
                    result.append('>');
                    i += 3;
                } else {
                    throw new IllegalArgumentException("Invalid entity definition.");
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    // TODO: Quick and dirty - add something more solid and replaces uses of this one with it
    public static Collection<ParagraphData> parseHTMLLike(ParagraphRenderStyle paragraphRenderStyle, String text) {
        if (text == null) {
            return Collections.emptyList();
        }
        StringReader reader = new StringReader(text);

        List<ParagraphData> result = new LinkedList<>();
        int character;
        try {
            StringBuilder sb = new StringBuilder();
            Font font = null;
            Color color = null;
            String hyperlink = null;

            HyperlinkParagraphData hyperlinkParagraphData = new HyperlinkParagraphData(paragraphRenderStyle);
            while ((character = reader.read()) != -1) {
                char c = (char) character;
                if (c == '\n') {
                    hyperlinkParagraphData.append(sb.toString(), new DefaultTextRenderStyle(font, color), hyperlink);
                    result.add(hyperlinkParagraphData);
                    sb.setLength(0);
                    hyperlinkParagraphData = new HyperlinkParagraphData(paragraphRenderStyle);
                    font = null;
                    color = null;
                    hyperlink = null;
                } else if (c == '<') {
                    char nextChar = (char) reader.read();
                    if (nextChar == '/') {
                        char id = (char) reader.read();
                        if (id == 'f') {
                            if (sb.length() > 0) {
                                hyperlinkParagraphData.append(sb.toString(), new DefaultTextRenderStyle(font, color), hyperlink);
                                sb.setLength(0);
                            }
                            font = null;
                        } else if (id == 'c') {
                            if (sb.length() > 0) {
                                hyperlinkParagraphData.append(sb.toString(), new DefaultTextRenderStyle(font, color), hyperlink);
                                sb.setLength(0);
                            }
                            color = null;
                        } else if (id == 'h') {
                            if (sb.length() > 0) {
                                hyperlinkParagraphData.append(sb.toString(), new DefaultTextRenderStyle(font, color), hyperlink);
                                sb.setLength(0);
                            }
                            hyperlink = null;
                        } else {
                            throw new IllegalArgumentException("Unable to parse text - " + text);
                        }
                        reader.read();
                    } else if (nextChar == 'f') {
                        if (sb.length() > 0) {
                            hyperlinkParagraphData.append(sb.toString(), new DefaultTextRenderStyle(font, color), hyperlink);
                            sb.setLength(0);
                        }
                        reader.read();
                        font = Assets.getFont(readUntilCharacter(reader, '>'));
                    } else if (nextChar == 'c') {
                        if (sb.length() > 0) {
                            hyperlinkParagraphData.append(sb.toString(), new DefaultTextRenderStyle(font, color), hyperlink);
                            sb.setLength(0);
                        }
                        reader.read();
                        color = new Color(Integer.parseInt(readUntilCharacter(reader, '>'), 16));
                    } else if (nextChar == 'h') {
                        if (sb.length() > 0) {
                            hyperlinkParagraphData.append(sb.toString(), new DefaultTextRenderStyle(font, color), hyperlink);
                            sb.setLength(0);
                        }
                        reader.read();
                        hyperlink = readUntilCharacter(reader, '>');
                    } else if (nextChar == 'l') {
                        readUntilCharacter(reader, '>');
                        sb.append('\n');
                    }
                } else if (c == '&') {
                    String escape = readUntilCharacter(reader, ';');
                    if (escape.equals("gt")) {
                        sb.append('>');
                    } else if (escape.equals("lt")) {
                        sb.append('<');
                    } else if (escape.equals("amp")) {
                        sb.append('&');
                    } else {
                        throw new IllegalArgumentException("Unknown escape sequence - " + escape);
                    }
                } else {
                    sb.append(c);
                }
            }

            if (sb.length() > 0) {
                hyperlinkParagraphData.append(sb.toString(), new DefaultTextRenderStyle(font, color), hyperlink);
            }
            result.add(hyperlinkParagraphData);
        } catch (IOException exp) {
            // Ignore - can't happen
        }

        return result;
    }

    private static String readUntilCharacter(StringReader reader, char c) throws IOException {
        StringBuilder sb = new StringBuilder();
        char read;
        do {
            read = (char) reader.read();
            if (read != c) {
                sb.append(read);
            }
        } while (read != c);
        return sb.toString();
    }

    private static final class DefaultTextRenderStyle implements TextRenderStyle {
        private Font font;
        private Color color;

        private DefaultTextRenderStyle(Font font, Color color) {
            this.font = font;
            this.color = color;
        }

        @Override
        public Font getFont(boolean isHyperlink) {
            return font;
        }

        @Override
        public Color getColor(boolean isHyperlink) {
            return color;
        }
    }
}
