package cz.zcu.kiv.md2odt.document.odt


import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.odftoolkit.simple.text.list.List
import org.w3c.dom.Node

/**
 * Created by pepe on 13. 3. 2017.
 */
class OdfSimpleWrapperTest {
    OdfSimpleWrapper odt

    @Before
    void setUp() throws Exception {
        odt = new OdfSimpleWrapper()
    }

    @Ignore
    @Test
    void example() throws Exception {
        odt.addParagraph("par")
        println(odt.lastNode.toString())
        odt.save("test.odt")
    }

    @Ignore
    @Test
    void templateExample() throws Exception {
        odt = new OdfSimpleWrapper("test.odt")

        odt.addHeading("nadpis 1 se stylem sablony",1)
        //odt.save("template_test.odt")
    }

    @Test
    void italicAllTest() throws Exception {
        odt.addParagraph("This <is< not <Sparta<!")
        odt.italicAll()
        assert odt.getLastNode().getTextContent().equals("This is not Sparta!")
    }

    @Test
    void boldAllTest() throws Exception {
        odt.addParagraph("This >is> not <Sp>art>a<!")
        odt.boldAll()
        assert odt.getLastNode().getTextContent().equals("This is not <Sparta<!")
    }

    @Test
    void linkAllTest() throws Exception {
        odt.addParagraph("#"+ OdfSimpleConstants.escape("https://www.seznam.cz/") +"@pokus#")
        odt.linkAll()
        assert odt.getLastNode().getTextContent().equals("pokus")
    }

    @Test
    void linkAllExceptionTest() throws Exception {
        odt.addParagraph("#"+ OdfSimpleConstants.escape("wwwsezn^amcz") +"@pokus#")
        odt.linkAll()
        assert odt.getLastNode().getTextContent().equals("pokus (wwwsezn^amcz) ")
    }

    @Test
    void inlineCodeAllTest() throws Exception {
        odt.addParagraph("'"+ OdfSimpleConstants.escape("this.add(something)") +"'")
        odt.inlineCodeAll()
        assert odt.getLastNode().getTextContent().equals("this.add(something)") && odt.getLastNode().getChildNodes().item(1).getAttributes().item(0).toString().startsWith("style-name=")
    }

    @Test
    void reEscapeAllTest() throws Exception {
        String inp = OdfSimpleConstants.escape("<&>&&<> &bold; text &bold; other") + "<italic<" + OdfSimpleConstants.escape("<non-Italic<")
        String exp = "<&>&&<> &bold; text &bold; other" + "italic" + "<non-Italic<"
        odt.addParagraph(inp)
        odt.italicAll()
        odt.reEscapeAll()
        assert odt.getLastNode().getTextContent().equals(exp)
    }

    @Test(expected = FileNotFoundException.class)
    void templateNotFound1() throws Exception {
        odt = new OdfSimpleWrapper("wrongtemplate.odt")
    }
    @Test(expected = FileNotFoundException.class)
    void templateNotFound2() throws Exception {
        odt = new OdfSimpleWrapper(new File("wrongtemplate.odt"))
    }

    @Ignore
    @Test
    void bulletListTest() {
        ArrayList<Object> list = new ArrayList<>()
        for (int i = 1; i < 4; i++) {
            list.add("Polozka " + i)
        }

        List odfList = odt.addList("Bullet list", OdfListEnum.BULLET_LIST)
        odt.addItemsToList(odfList, list)
        odt.save("test.odt")
    }

    @Ignore
    @Test
    void numberedListTest() {
        ArrayList<String> list = new ArrayList<>()
        for (int i = 1; i < 4; i++) {
            list.add("Polozka " + i)
        }

        List odfList = odt.addList("Numbered list", OdfListEnum.NUMBERED_LIST)
        odt.addItemsToList(odfList, list)
        odt.save("test.odt")
    }

    @Ignore
    @Test
    void subListTest() {
        ArrayList<String> list = new ArrayList<>()
        for (int i = 1; i < 4; i++) {
            list.add("Polozka " + i)
        }

        List odfList = odt.addList("Nested list", OdfListEnum.NUMBERED_LIST)
        odt.addItemsToList(odfList, list)
        List nestedList = odt.addSubList(odfList, OdfListEnum.BULLET_LIST)
        odt.addItemsToList(nestedList, list)
        odt.save("test.odt")
    }
}
