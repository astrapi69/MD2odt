package cz.zcu.kiv.md2odt.document.odfdom

import cz.zcu.kiv.md2odt.document.*
import cz.zcu.kiv.md2odt.highlight.CodeParser
import cz.zcu.kiv.md2odt.highlight.CodeSectionTypeColorHandler
import cz.zcu.kiv.md2odt.highlight.Parser
import cz.zcu.kiv.md2odt.highlight.content.CodeSection
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import org.apache.log4j.Logger
import org.odftoolkit.odfdom.dom.OdfContentDom
import org.odftoolkit.odfdom.dom.OdfSchemaDocument
import org.odftoolkit.odfdom.dom.OdfStylesDom
import org.odftoolkit.odfdom.dom.attribute.style.StyleFamilyAttribute
import org.odftoolkit.odfdom.dom.attribute.style.StyleFontNameAttribute
import org.odftoolkit.odfdom.dom.attribute.style.StyleNameAttribute
import org.odftoolkit.odfdom.dom.attribute.style.StyleParentStyleNameAttribute
import org.odftoolkit.odfdom.dom.attribute.style.StyleTextLineThroughStyleAttribute
import org.odftoolkit.odfdom.dom.attribute.style.StyleTextLineThroughTypeAttribute
import org.odftoolkit.odfdom.dom.attribute.table.TableStyleNameAttribute
import org.odftoolkit.odfdom.dom.attribute.text.TextStyleNameAttribute
import org.odftoolkit.odfdom.dom.element.draw.DrawFrameElement
import org.odftoolkit.odfdom.dom.element.draw.DrawImageElement
import org.odftoolkit.odfdom.dom.element.style.StyleStyleElement
import org.odftoolkit.odfdom.dom.element.style.StyleTextPropertiesElement
import org.odftoolkit.odfdom.dom.element.text.TextAElement
import org.odftoolkit.odfdom.dom.element.text.TextLineBreakElement
import org.odftoolkit.odfdom.dom.element.text.TextListItemElement
import org.odftoolkit.odfdom.dom.element.text.TextPElement
import org.odftoolkit.odfdom.dom.element.text.TextSpanElement
import org.odftoolkit.odfdom.pkg.OdfElement
import org.odftoolkit.odfdom.pkg.OdfPackage
import org.odftoolkit.odfdom.pkg.manifest.OdfFileEntry
import org.odftoolkit.odfdom.type.Color
import org.odftoolkit.odfdom.type.StyleName
import org.odftoolkit.simple.TextDocument
import org.odftoolkit.simple.draw.Image
import org.odftoolkit.simple.style.Font
import org.odftoolkit.simple.style.StyleTypeDefinitions
import org.odftoolkit.simple.style.StyleTypeDefinitions.FontStyle
import org.odftoolkit.simple.table.Cell
import org.odftoolkit.simple.table.Table
import org.odftoolkit.simple.text.Paragraph
import org.odftoolkit.simple.text.Span
import org.odftoolkit.simple.text.list.BulletDecorator
import org.odftoolkit.simple.text.list.List as OdfList
import org.odftoolkit.simple.text.list.ListDecorator
import org.odftoolkit.simple.text.list.NumberDecorator
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.Text

import java.util.function.ToIntFunction

/**
 * Created by pepe on 5. 4. 2017.
 */
class OdfdomDocument implements DocumentAdapter{

    protected final TextDocument odt
    private static final Logger LOGGER = Logger.getLogger(OdfdomDocument)
    protected String strikeStyleName, subScriptStyleName, superScriptStyleName
    java.awt.Color codeBlockBackgroundColor;
    OdfdomDocument() {
        odt = TextDocument.newTextDocument()
        odt.getParagraphByIndex(0,false) .remove()     //removes an empty paragraph
        prepareConstructor()
    }

    OdfdomDocument(File file) {
        this.odt = TextDocument.loadDocument(file)
        fillDefaultStyles()
        prepareConstructor()
    }

    OdfdomDocument(String documentPath) {
        this.odt = TextDocument.loadDocument(documentPath)
        fillDefaultStyles()
        prepareConstructor()
    }

    OdfdomDocument(InputStream inputStream) {
        this.odt = TextDocument.loadDocument(inputStream)
        fillDefaultStyles()
        prepareConstructor()
    }

    protected void prepareConstructor() {
        addTextStyles()
        try {
            codeBlockBackgroundColor = getCodeBlockBackgroundColor()
        }
        catch (Exception e) {
            codeBlockBackgroundColor = java.awt.Color.WHITE
            LOGGER.error("getCodeBlockBackgroundColor failed: "+e)
        }
    }

    java.awt.Color getCodeBlockBackgroundColor() {
        NodeList nl = odt.stylesDom.officeStyles.getElementsByTagName("style:style")
        for (int i = 0; i < nl.length; i++) {
            Node n = nl.item(i).getAttributes().getNamedItem("style:name")
            if(n != null && n.textContent.equals(StyleNames.CODE.getValue())) {
                //style:paragraph-properties fo:background-color="#888a85"
                for (Node spp : nl.item(i).childNodes) {
                    if (spp != null && spp.nodeName.equals("style:paragraph-properties")) {
                        Node fbc = spp.attributes.getNamedItem("fo:background-color")
                        if (fbc != null) {
                            String s = fbc.textContent.replaceAll("[^a-fA-F0-9]","")
                            int rgb = Integer.parseInt(s, 16)
                            return new java.awt.Color(rgb)
                        }
                        return java.awt.Color.WHITE
                    }
                }
                return java.awt.Color.WHITE
            }
        }
        return java.awt.Color.WHITE
    }

    protected StyleTextPropertiesElement addStyleStyleElementForSpan(String styleName) {
        def oas = odt.contentDom.getOrCreateAutomaticStyles()
        StyleStyleElement sse = new StyleStyleElement(odt.getContentDom())
        oas.appendChild(sse)
        sse.setStyleNameAttribute(styleName)
        sse.setStyleFamilyAttribute("text")

        StyleTextPropertiesElement stpe = new StyleTextPropertiesElement(odt.getContentDom())
        sse.appendChild(stpe)
        return stpe
    }

    protected void addStrikeStyle() {
        strikeStyleName = "BHM_MD2odt_Strike"
        def stpe = addStyleStyleElementForSpan(strikeStyleName)
        stpe.setStyleTextLineThroughTypeAttribute("single")
        stpe.setStyleTextLineThroughStyleAttribute("solid")
    }

    protected void addSubScriptStyle() {
        subScriptStyleName = "BHM_MD2odt_SubScript"
        def stpe = addStyleStyleElementForSpan(subScriptStyleName)
        stpe.setStyleTextPositionAttribute("sub 58%")
    }

    protected void addSuperScriptStyle() {
        superScriptStyleName = "BHM_MD2odt_SuperScript"
        def stpe = addStyleStyleElementForSpan(superScriptStyleName)
        stpe.setStyleTextPositionAttribute("super 58%")
    }

    protected void addTextStyles() {
        addStrikeStyle()
        addSubScriptStyle()
        addSuperScriptStyle()
        addInlineCodeFont()
    }

    protected void addInlineCodeFont() {
        Span s = new Span(new TextSpanElement(odt.getContentDom()))
        s.getStyleHandler().getTextPropertiesForWrite().setFont(new Font("Courier New", StyleTypeDefinitions.FontStyle.REGULAR, 12))
        // span is not added to document it only fills the style and set FONT NAME
    }

    protected Set<String> getNamedItemValues(NodeList nl, String name) {
        Set<String> set = new TreeSet<>()

        for (int i = 0; i < nl.length; i++) {
            Node n = nl.item(i).getAttributes().getNamedItem(name)
            if(n != null)
                set.add(n.getNodeValue())
        }
        return set
    }

    protected Set<String> getStylesStyleNames(OdfStylesDom stylesDom) {
        NodeList nl = stylesDom.getOfficeStyles().getElementsByTagName("style:style")
        return getNamedItemValues(nl, "style:name")
    }

    protected void fillDefaultStyles() {
        Set<String> odtStyleNames = getStylesStyleNames(odt.getStylesDom())

        TextDocument defaultTextDocument = TextDocument.newTextDocument()

        NodeList nl = defaultTextDocument.getStylesDom().getOfficeStyles().getElementsByTagName("style:style")
        for (int i = 0; i < nl.length; i++) {
            Node node = nl.item(i)
            String styleName = node.getAttributes().getNamedItem("style:name").getNodeValue()

            if(!odtStyleNames.contains(styleName)) {
                Node n = odt.getStylesDom().importNode(node, true)
                odt.getStylesDom().getOfficeStyles().appendChild(n)
            }
        }
    }

    protected void appendText(OdfElement element, String text) {

        String[] s = text.split("\n")

        if (s == null || s.length < 1) {
            for (int i = 0; i < text.length(); i++) {
                TextLineBreakElement tlbe = new TextLineBreakElement(odt.contentDom)
                element.appendChild(tlbe)
            }
            return
        }

        Text textNode = odt.getContentDom().createTextNode(s[0])
        element.appendChild(textNode)

        for (int i = 1; i < s.length; i++) {
            TextLineBreakElement tlbe = new TextLineBreakElement(odt.contentDom)
            element.appendChild(tlbe)

            textNode = odt.getContentDom().createTextNode(s[i])
            element.appendChild(textNode)
        }

        if (text.endsWith("\n")) {
            TextLineBreakElement tlbe = new TextLineBreakElement(odt.contentDom)
            element.appendChild(tlbe)
        }
    }

    protected Span appendSpan(OdfElement element) {
        Span s = new Span(new TextSpanElement(odt.getContentDom()))
        element.appendChild(s.getOdfElement())
        return s
    }

    protected Span appendCodeSpan(OdfElement element) {
        Span s = appendSpan(element)
        s.getStyleHandler().getTextPropertiesForWrite().setFontName("Courier New")
        return s
    }

    protected Span appendBoldSpan(OdfElement element) {
        Span s = appendSpan(element)
        s.getStyleHandler().getTextPropertiesForWrite().setFontStyle(StyleTypeDefinitions.FontStyle.BOLD)
        return s
    }

    protected Span appendItalicSpan(OdfElement element) {
        Span s = appendSpan(element)
        s.getStyleHandler().getTextPropertiesForWrite().setFontStyle(StyleTypeDefinitions.FontStyle.ITALIC)
        return s
    }

    protected Span appendStrikeSpan(OdfElement element) {
        Span s = appendSpan(element)
        setTextStyleNameAttr(s.odfElement, strikeStyleName)
        return s
    }

    protected Span appendSubScriptSpan(OdfElement element) {
        Span s = appendSpan(element)
        setTextStyleNameAttr(s.odfElement, subScriptStyleName)
        return s
    }

    protected Span appendSuperScriptSpan(OdfElement element) {
        Span s = appendSpan(element)
        setTextStyleNameAttr(s.odfElement, superScriptStyleName)
        return s
    }

    protected void appendLink(OdfElement element, ParagraphContent content, String url) {
        TextAElement aElement = (TextAElement) odt.getContentDom().newOdfElement(TextAElement.class)
        aElement.setXlinkTypeAttribute("simple")
        aElement.setXlinkHrefAttribute(url)
        fillWithParagraphContent(aElement, content)
        element.appendChild(aElement)
    }

    protected void convertSvgToPng(InputStream input, OutputStream output) {
        LOGGER.debug("IMAGE converting SVG to PNG START")
        TranscoderInput input_svg_image = new TranscoderInput(input)
        //Step-2: Define OutputStream to PNG Image and attach to TranscoderOutput
        TranscoderOutput output_png_image = new TranscoderOutput(output)
        // Step-3: Create PNGTranscoder and define hints if required
        PNGTranscoder my_converter = new PNGTranscoder();
        // Step-4: Convert and Write output
        my_converter.transcode(input_svg_image, output_png_image);
        // Step 5- close / flush Output Stream
        output.flush()
        output.close()

        input.close()
        LOGGER.debug("IMAGE converting SVG to PNG DONE")
    }

    protected String appendImageHandleSvg(String packagePath, OdfSchemaDocument mOdfSchemaDoc1) {
        LOGGER.debug("IMAGE handling SVG START")

        def inp = mOdfSchemaDoc1.getPackage().getInputStream(packagePath)
        packagePath = packagePath.substring(0, packagePath.length() - 3) + "png"

        LOGGER.debug("IMAGE recognized as SVG will be converted to: "+packagePath)

        def out = mOdfSchemaDoc1.getPackage().insertOutputStream(packagePath, "image/png")
        convertSvgToPng(inp, out)

        LOGGER.debug("IMAGE handling SVG DONE")
        return packagePath
    }

    protected void appendImage(OdfElement element, String url) {
        LOGGER.debug("IMAGE START apending from URL: " + url)

        DrawFrameElement frame = new DrawFrameElement(odt.getContentDom())
        DrawImageElement e1 = frame.newDrawImageElement()

        URI imageUri = new URI(url)
        String imageRef1 = imageUri.toString()
        String mediaType1 = OdfFileEntry.getMediaTypeString(imageRef1)
        OdfSchemaDocument mOdfSchemaDoc1 = (OdfSchemaDocument) odt.getContentDom().getDocument()
        String packagePath = getImagePath(mOdfSchemaDoc1, imageRef1)
        mOdfSchemaDoc1.getPackage().insert(imageUri, packagePath, mediaType1)

        if (packagePath.endsWith("svg")) {
            packagePath = appendImageHandleSvg(packagePath, mOdfSchemaDoc1)
        }

        packagePath = packagePath.replaceFirst(odt.getContentDom().getDocument().getDocumentPath(), "")
        Image.configureInsertedImage((OdfSchemaDocument) odt.getContentDom().getDocument(), e1, packagePath, false)
        Image mImage = Image.getInstanceof(e1)
        mImage.getStyleHandler().setAchorType(StyleTypeDefinitions.AnchorType.AS_CHARACTER)

        element.appendChild(frame)
        LOGGER.debug("IMAGE DONE apending from URL")
    }

    protected void appendImageFromStream(OdfElement element, String url, InputStream inputStream) {

        LOGGER.debug("IMAGE START apending from STREAM, url: " + url)

        DrawFrameElement frame = new DrawFrameElement(odt.getContentDom())
        DrawImageElement e1 = frame.newDrawImageElement()

        String imageRef1 = url
        String mediaType1 = OdfFileEntry.getMediaTypeString(imageRef1)
        OdfSchemaDocument mOdfSchemaDoc1 = (OdfSchemaDocument) odt.getContentDom().getDocument()
        String packagePath = getImagePath(mOdfSchemaDoc1, imageRef1)
        mOdfSchemaDoc1.getPackage().insert(inputStream, packagePath, mediaType1)

        if (packagePath.endsWith("svg")) {
            packagePath = appendImageHandleSvg(packagePath, mOdfSchemaDoc1)
        }

        packagePath = packagePath.replaceFirst(odt.getContentDom().getDocument().getDocumentPath(), "")
        Image.configureInsertedImage((OdfSchemaDocument) odt.getContentDom().getDocument(), e1, packagePath, false)
        Image mImage = Image.getInstanceof(e1)
        mImage.getStyleHandler().setAchorType(StyleTypeDefinitions.AnchorType.AS_CHARACTER)

        element.appendChild(frame)
        LOGGER.debug("IMAGE DONE apending from STREAM")
    }

    protected String getImagePath(OdfSchemaDocument mOdfSchemaDoc, String imageRef) {
        def fullPath

        try {
            URL url = new URL(imageRef)
            fullPath = url.getHost() + url.getPath()
            LOGGER.debug("IMAGE_PATH path is url: " + fullPath)
        }
        catch (Exception e) {
            fullPath = imageRef
            LOGGER.debug("IMAGE_PATH path is not url: " + fullPath)
        }

        if(fullPath.contains("//")) {
            fullPath = fullPath.substring(fullPath.lastIndexOf("//") + 2, fullPath.length())
        }
        if(fullPath.startsWith("/")) {
            fullPath = fullPath.substring(1, fullPath.length())
        }
        if(fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0, fullPath.length()-1)
        }

        String name = fullPath
        String path = ""
        if(fullPath.contains("/")) {
            name = fullPath.substring(fullPath.lastIndexOf("/"), fullPath.length())
            path = fullPath.substring(0, fullPath.lastIndexOf("/")).replaceAll("[^a-zA-Z0-9/.-]", "_")
        }

        String packagePath = mOdfSchemaDoc.getDocumentPath() + OdfPackage.OdfFile.IMAGE_DIRECTORY.getPath() + "/" + path + name

        LOGGER.debug("IMAGE_PATH: '" + imageRef + "' converted to '" + packagePath + "'")

        return packagePath
    }

    protected void fillWithParagraphContent(OdfElement element, ParagraphContent paragraphContent) {
        if (paragraphContent == null) {
            LOGGER.info("ParagraphContent is null in fillWithParagraphContent() method.")
            return
        }
        for(SpanContent sc : paragraphContent.list) {
            if(sc == null) {
                LOGGER.info("SpanContent is null in fillWithParagraphContent() method.")
                continue
            }

            switch (sc.getType()) {
               case SpanType.TEXT:
                    if (sc instanceof SpanContentText) {
                        if (sc.styles.isEmpty()) {
                            appendText(element, sc.getText())
                        }
                        else {
                            OdfElement e = element
                            Span s

                            for (TextStyle ts : sc.styles ) {
                                switch (ts) {
                                    case TextStyle.ITALIC:
                                        s = appendItalicSpan(e)
                                        e = s.odfElement
                                        break
                                    case TextStyle.BOLD:
                                        s = appendBoldSpan(e)
                                        e = s.odfElement
                                        break
                                    case TextStyle.CODE:
                                        s = appendCodeSpan(e)
                                        e = s.odfElement
                                        break
                                    case TextStyle.STRIKE:
                                        s = appendStrikeSpan(e)
                                        e = s.odfElement
                                        break
                                    case TextStyle.SUBSCRIPT:
                                        s = appendSubScriptSpan(e)
                                        e = s.odfElement
                                        break
                                    case TextStyle.SUPERSCRIPT:
                                        s = appendSuperScriptSpan(e)
                                        e = s.odfElement
                                        break
                                    default:
                                        LOGGER.warn("TextStyle not implemented: " + ts + " in class " + sc.class)
                                }
                            }
                            appendText(e, sc.getText())
                        }
                    } else {
                        LOGGER.error("SpanContent with a '" + sc.getType() + "' type and instance of '" + sc.class + "'")
                    }
                    break

                case SpanType.LINK:
                    if (sc instanceof SpanContentLink) {
                        appendLink(element, sc.content, sc.url)
                    } else {
                        LOGGER.error("SpanContent with a '" + sc.getType() + "' type and instance of '" + sc.class + "'")
                    }
                    break

                case SpanType.IMAGE:
                    LOGGER.debug("IMAGE: START handling SpanType.IMAGE")
                    if (sc instanceof SpanContentImageLocal) {
                        try {
                            appendImageFromStream(element, sc.getUrl(), sc.getStream())
                        }
                        catch (Exception e) {
                            LOGGER.info("Exception while inserting image from stream in OdfdomDocument: " + e.toString())
                            appendText(element, sc.getAlt())
                        }
                        finally{
                            if (sc.getStream())
                                sc.getStream().close()
                        }
                    } else if (sc instanceof SpanContentImage) {
                        try {
                            appendImage(element, sc.getUrl())
                        }
                        catch (Exception e) {
                            LOGGER.info("Exception while inserting image in OdfdomDocument: " + e.toString())
                            appendText(element, sc.getAlt())
                        }
                    } else {
                        LOGGER.error("SpanContent with a '" + sc.getType() + "' type and instance of '" + sc.class + "'")
                    }
                    LOGGER.debug("IMAGE: DONE handling SpanType.IMAGE")
                    break
                default:
                    LOGGER.warn("SpanType not implemented: " + sc.getType() + " in class " + sc.class)
            }
        }
    }

    protected void setTextStyleNameAttr(OdfElement element, String styleName) {
        TextStyleNameAttribute attr = new TextStyleNameAttribute(odt.getContentDom())
        element.setOdfAttribute(attr)
        attr.setValue(styleName)
    }

    protected Paragraph addParagraph(String styleName) {
        return addParagraph("", styleName)
    }

    protected Paragraph addParagraph(String text, String styleName) {
        Paragraph paragraph = odt.addParagraph(text)
        setTextStyleNameAttr(paragraph.getOdfElement(), styleName)
        return paragraph
    }

    @Override
    void addHeading(String text, int level) {
        def paragraph = addParagraph(text, StyleNames.HEADING.getLevel(level))
        paragraph.applyHeading(true, level)
    }

    @Override
    void addParagraph(ParagraphContent content) {
        Paragraph paragraph = addParagraph(StyleNames.BODY_TEXT.getValue())
        fillWithParagraphContent(paragraph.getOdfElement(), content)
    }

    @Override
    void addCodeBlock(String code) {
        addParagraph(code, StyleNames.CODE.getValue())
    }

    private void appendColorSpan(OdfElement element, String text, java.awt.Color color) {
        Span s = new Span(new TextSpanElement(odt.getContentDom()))
        appendText(s.odfElement, text)
        s.getStyleHandler().getTextPropertiesForWrite().setFontColor(new Color(color))
        element.appendChild(s.getOdfElement())
    }

    @Override
    void addCodeBlock(String code, String lang) {
        if (lang == null || lang.equals("")) {
            addCodeBlock(code)
            return
        }
        CodeParser codeParser = new CodeParser()
        if (!codeParser.isKnownLanguage(lang)) {
            LOGGER.info("addCodeBlock(String code, String lang): lang '"+lang+"' is not known, code is not formatted")
            addCodeBlock(code)
            return
        }

        CodeSectionTypeColorHandler colorHandler = new CodeSectionTypeColorHandler(codeBlockBackgroundColor)
        def parElement = addParagraph(StyleNames.CODE.getValue()).getOdfElement()
        List<CodeSection> codeSections = codeParser.parse(code, lang)
        for (CodeSection cs : codeSections) {
            appendColorSpan(parElement, cs.getText(), colorHandler.handle(cs.getType()))
        }
    }

    @Override
    void addQuoteBlock(List<ParagraphContent> paragraphs) {
        for (ParagraphContent pc : paragraphs) {
            def paragraph = addParagraph(StyleNames.QUOTE.getValue())
            fillWithParagraphContent(paragraph.getOdfElement(), pc)
        }
    }

    @Override
    void addHorizontalRule() {
        addParagraph(StyleNames.HORIZONTAL_RULE.getValue())
    }

    private ListDecorator switchDecorator(ListType e) {
        ListDecorator decorator = null
        switch (e) {
            case ListType.BULLET:
                decorator = new BulletDecorator(odt)
                break
            case ListType.ORDERED:
                decorator = new NumberDecorator(odt)
                break
        }
        return decorator
    }

    @Override
    void addList(ListContent content) {
        OdfList list = odt.addList()
        fillList(content, list)
    }

    protected void fillList(ListContent content, OdfList list) {
        list.setDecorator(switchDecorator(content.getType()))
        //newList.setHeader(listHeading)
        addListRec(content, list)
    }

    private void addListRec(ListContent content, OdfList list) {
        List<List<BlockContent>> listListBlockContent = content.getListItems()

        for (List<BlockContent> listBlock : listListBlockContent) {
            TextListItemElement textItem = null

            for (BlockContent blockContent : listBlock) {
                if(blockContent instanceof ParagraphContent) {
                    if(textItem == null) {
                        textItem = list.getOdfElement().newTextListItemElement()
                    }
                    TextPElement par = textItem.newTextPElement()
                    setTextStyleNameAttr(par, StyleNames.LIST.getValue())
                    fillWithParagraphContent(par, blockContent)
                }
                else if(blockContent instanceof ListContent) {
                    OdfList newList = addSubList(list, blockContent.getType())
                    addListRec(blockContent, newList)
                }
            }
        }
    }

    OdfList addSubList(OdfList parentList, ListType e) {
        ListDecorator decorator = switchDecorator(e)
        return parentList.getItem(parentList.size() - 1).addList(decorator)
    }

    protected void setTableStyleNameAttr(OdfElement element, String styleName) {
        TableStyleNameAttribute attr = new TableStyleNameAttribute(odt.getContentDom())
        element.setOdfAttribute(attr)
        attr.setValue(styleName)
    }

    @Override
    void addTable(TableContent content) {
        List<List<TableCellContent>> rows = content.getRows()
        int rowCount = rows.size()
        int colCount = 0
        for (List<TableCellContent> cols : rows) {
            if (cols.size()>colCount) {
                colCount = cols.size()
            }
        }

        Table table=Table.newTable(odt, rowCount, colCount)
        table.setVerticalMargin(0.1, 0.1)

        int r = 0
        for (List<TableCellContent> row : rows) {
            int c = 0
            for (TableCellContent tableCellContent : row) {
                Cell cell = table.getCellByPosition(c, r)
                def par = cell.addParagraph("")
                fillWithParagraphContent(par.odfElement, tableCellContent.content)
                if (tableCellContent.heading) {
                    setTableStyleNameAttr(cell.odfElement, StyleNames.TABLE_HEADING.getValue())
                }
                else {
                    setTableStyleNameAttr(cell.odfElement, StyleNames.TABLE_CONTENTS.getValue())
                }

                switch (tableCellContent.align) {
                    case TableCellContent.Align.LEFT:
                        par.setHorizontalAlignment(StyleTypeDefinitions.HorizontalAlignmentType.LEFT)
                        break
                    case TableCellContent.Align.CENTER:
                        par.setHorizontalAlignment(StyleTypeDefinitions.HorizontalAlignmentType.CENTER)
                        break
                    case TableCellContent.Align.RIGHT:
                        par.setHorizontalAlignment(StyleTypeDefinitions.HorizontalAlignmentType.RIGHT)
                        break
                }
                c++
            }
            r++
        }
    }

    @Override
    void save(String documentPath) {
        odt.save(documentPath)
    }

    @Override
    void save(OutputStream outputStream) {
        odt.save(outputStream)
        println()
    }
}
