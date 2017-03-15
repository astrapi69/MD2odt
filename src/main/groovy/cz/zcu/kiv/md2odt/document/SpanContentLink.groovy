package cz.zcu.kiv.md2odt.document

import groovy.transform.Immutable

/**
 *
 * @version 2017-03-15
 * @author Patrik Harag
 */
@Immutable
class SpanContentLink implements SpanContent {

    String text
    String url
    SpanType type

}
