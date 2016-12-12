/**
 * Created by suanmiao on 19/11/2016.
 */
const TYPE_PARAGRAPH = 1;
const TYPE_HEADING_1 = 2;
const TYPE_HEADING_2 = 3;
const TYPE_HEADING_3 = 4;
const TYPE_HEADING_4 = 5;
const TYPE_HEADING_5 = 6;
const TYPE_QUOTE = 7;
const TYPE_LI = 8;
const TYPE_LIST = 9;

const TYPE_BOLD = 10;
const TYPE_ITALIC = 11;
const TYPE_IMG = 12;
const TYPE_HYPER = 13;
const TYPE_UNDER_LINE = 14;
const TYPE_DELETE = 15;
const TYPE_BR = 16;
const TYPE_CODE = 17;

const TYPE_BODY = 100;

var log_enable = false;

function parse(raw_text) {
    var lines = raw_text.split("\n");
    var line_list = pre_line_parse(lines);
    var body = line_parse(line_list);
    body.parse_inside(0);
    var html = body.generate_html().html();
    log(html)
    return html;
}

function pre_line_parse(lines) {
    var line_list = [];
    var pre_depth = 0;
    for (var i = 0; i < lines.length; i++) {
        var line = lines[i];
        var whitespaceCount = countWhitespace(line);
        var depth = parseInt(whitespaceCount / 3);
        var type = getElementType(whitespaceCount, line);
        switch (type) {
            //for cross-line elements which violate depth rule
            case TYPE_QUOTE:
                if (pre_depth > depth) {
                    //end of quote
                    pre_depth = depth;
                } else {
                    //start of quote
                    pre_depth = depth + 1;
                }
                line_list.push(generate_line_object(type, line, depth, whitespaceCount));
                break;
            //other elements
            default:
                if(pre_depth > 0){
                    line_list.push(generate_line_object(type, line, pre_depth, whitespaceCount));
                }else{
                    line_list.push(generate_line_object(type, line, depth, whitespaceCount));
                }
                break;
        }
    }

    // for(var i = 0; i < line_list.length; i++){
    //     var line_object = line_list[i];
    //     log("line " + i + ", type " + line_object.type + ", depth " + line_object.depth + ", raw " + line_object.raw_text);
    // }
    // log("---------------------------------------");
    return line_list;
}

function line_parse(line_list) {
    var body = new BODYElement();
    var stack = [];
    for (var i = 0; i < line_list.length; i++) {
        var line_object = line_list[i];
        var element = generateElement(line_object.type, line_object.raw_text, line_object.depth, line_object.whitespace_count);
        var depth = line_object.depth;
        if (isGeneticType(line_object.type)) {
            element = generateElement(TYPE_PARAGRAPH, line_object.raw_text, line_object.depth, line_object.whitespace_count);
        }
        if (stack.length == 1 && stack[0].type == TYPE_PARAGRAPH && stack[0].depth == 0 && stack[0].raw_text.length > 0) {
            body.append_child(generateElement(TYPE_BR, "", 0, 0));
        }
        if (stack.length == 0) {
            if (stack.length == 0) {
                body.append_child(element);
            }
            stack.push(element);
        } else {
            if (depth < stack.length) {
                //end previous depth
                while (stack.length > depth) {
                    stack.pop().end_line = i - 1;
                }
                if (stack.length > 0) {
                    stack[stack.length - 1].append_child(element);
                } else {
                    if (stack.length == 0) {
                        body.append_child(element);
                    }
                    stack.push(element);
                }
            } else {
                stack[stack.length - 1].append_child(element);
                stack.push(element);
            }
        }
    }
    while (stack.length > 0) {
        stack.pop().end_line = line_list.length - 1;
    }
    print_elements(body, 0);
    log("-----------------------");
    return body;
}

var parse_inside = function (total_depth) {
    if (this.parsed) {
        return;
    }

    switch (this.type) {
        case TYPE_HEADING_1:
        case TYPE_HEADING_2:
        case TYPE_HEADING_3:
        case TYPE_HEADING_4:
        case TYPE_HEADING_5:
            var start_count = 0;
            while (start_count < 6 && start_count < this.raw_text.length && this.raw_text.charAt(start_count) == '#') {
                start_count++;
            }
            this.display_text = this.raw_text.substring(start_count);
            break;
        case TYPE_LI:
            var raw_string = this.raw_text.substring(this.whitespace_count + 2);
            var pElement = generateElement(TYPE_PARAGRAPH, raw_string, 0, this.whitespace_count);
            var prepend_child = [];
            prepend_child.push(pElement);
            this.children = prepend_child.concat(this.children);
            break;
        case TYPE_PARAGRAPH:
            var has_child = false;
            var ongoing_str = "";
            var ongoing_type = TYPE_PARAGRAPH;

            for (var i = 0; i < this.raw_text.length; i++) {
                var char = this.raw_text.charAt(i);
                var geneticHolder = getGeneticType(i, this.raw_text);
                var genetic_type = geneticHolder.type;
                if (genetic_type != TYPE_PARAGRAPH) {
                    has_child = true;
                    if (genetic_type == TYPE_HYPER || genetic_type == TYPE_IMG) {
                        if (ongoing_str.length > 0) {
                            var element = generateElement(ongoing_type, ongoing_str);
                            element.display_text = ongoing_str;
                            element.parsed = true;
                            this.append_child(element);
                        }
                        var element = generateElement(genetic_type, "");
                        element.parsed = true;
                        element.display_text = geneticHolder.name;
                        element.url = geneticHolder.url;
                        this.append_child(element);
                        ongoing_type = TYPE_PARAGRAPH;
                        //ongoing type should be paragraph after match
                    } else {
                        if (ongoing_type == genetic_type) {
                            var element = generateElement(ongoing_type, ongoing_str);
                            element.display_text = ongoing_str;
                            element.parsed = true;
                            this.append_child(element);
                            ongoing_type = TYPE_PARAGRAPH;
                        } else {
                            var element = generateElement(ongoing_type, ongoing_str);
                            element.display_text = ongoing_str;
                            element.parsed = true;
                            this.append_child(element);
                            ongoing_type = genetic_type;
                        }
                    }
                    ongoing_str = "";
                    i += (geneticHolder.skip - 1);
                } else {
                    ongoing_str += char;
                }
            }

            if (!has_child) {
                this.display_text = this.raw_text;
            } else {
                var element = generateElement(ongoing_type, ongoing_str);
                element.display_text = ongoing_str;
                element.parsed = true;
                this.append_child(element);
            }
            break;
        case TYPE_QUOTE:
            var code_type = is_code_quote(this.raw_text);
            if (code_type != undefined && code_type != null) {
                var code_str = "";
                for (var i = 0; i < this.children.length; i++) {
                    code_str += this.children[i].raw_text;
                    if (i != this.children.length - 1) {
                        code_str += "\n";
                    }
                }
                this.children = [];
                var codeElement = generateElement(TYPE_CODE, code_str, 0, 0);
                codeElement.display_text = code_str;
                codeElement.code_type = code_type;
                this.append_child(codeElement);
            }
            break;
    }
    //special case for consecutive li elements
    var listElement = undefined;
    for (var index = 0; index < this.children.length; index++) {
        var childElement = this.children[index];
        childElement.parse_inside(total_depth + 1);
        if (childElement.type == TYPE_LI) {
            if (listElement == undefined) {
                listElement = generateElement(TYPE_LIST, "", this.depth);
                listElement.append_child(childElement);
                this.children[index] = listElement;
            } else {
                this.children.splice(index, 1);
                listElement.append_child(childElement);
                index--;
            }
        } else {
            listElement = undefined;
        }
    }
    this.parsed = true;
}

function generate_line_object(type, raw_text, depth, whitespace_count) {
    var line_object = new Object();
    line_object.raw_text = raw_text;
    line_object.depth = depth;
    line_object.type = type;
    line_object.whitespace_count = whitespace_count;
    return line_object;
}


function print_elements(element, index) {
    var depth_str = "";
    while (depth_str.length < element.depth) {
        depth_str += "-";
    }
    if (element.depth != undefined) {
        log(depth_str + "[" + index + "]" + element.to_string())
    }
    for (var i = 0; i < element.children.length; i++) {
        var child = element.children[i]
        print_elements(child, i)
    }
}

function generateElement(type, raw_text, depth, whitespace_count) {
    var element = null;
    switch (type) {
        case TYPE_HEADING_1:
            element = new H1Element(raw_text, depth, whitespace_count);
            break;
        case TYPE_HEADING_2:
            element = new H2Element(raw_text, depth, whitespace_count);
            break;
        case TYPE_HEADING_3:
            element = new H3Element(raw_text, depth, whitespace_count);
            break;
        case TYPE_HEADING_4:
            element = new H4Element(raw_text, depth, whitespace_count);
            break;
        case TYPE_HEADING_5:
            element = new H5Element(raw_text, depth, whitespace_count);
            break;
        case TYPE_QUOTE:
            element = new QUOTElement(raw_text, depth, whitespace_count);
            break;
        case TYPE_LI:
            element = new LIElement(raw_text, depth, whitespace_count);
            break;
        case TYPE_LIST:
            element = new LISTElement(raw_text, depth, whitespace_count);
            break;
        case TYPE_BODY:
            element = new BODYElement();
            break;
        case TYPE_BOLD:
            element = new BOLDElement(raw_text, depth, whitespace_count);
            break;
        case TYPE_ITALIC:
            element = new ITALICElement(raw_text, depth, whitespace_count);
            break;
        case TYPE_UNDER_LINE:
            element = new UNDERLINEElement(raw_text, depth, whitespace_count);
            break;
        case TYPE_DELETE:
            element = new DELETEElement(raw_text, depth, whitespace_count);
            break;
        case TYPE_HYPER:
            element = new HYPERTEXTElement(raw_text, depth, whitespace_count);
            break;
        case TYPE_IMG:
            element = new IMGElement(raw_text, depth, whitespace_count);
            break;
        case TYPE_BR:
            element = new BRElement(raw_text, depth, whitespace_count);
            break;
        case TYPE_CODE:
            element = new CODEElement(raw_text, depth, whitespace_count);
            break;
        default:
            element = new PElement(raw_text, depth, whitespace_count);
            break;
    }
    return element;
}

function getGeneticType(start, line) {
    var geneticHolder = new Object();
    if (start < line.length) {
        var beginStr = line.substr(start, Math.min(line.length, start + 6));
        if (beginStr.startsWith("**")) {
            //bold
            geneticHolder.type = TYPE_BOLD;
            geneticHolder.skip = 2;
            return geneticHolder;
        } else if (beginStr.startsWith("*")) {
            geneticHolder.type = TYPE_ITALIC;
            geneticHolder.skip = 1;
            return geneticHolder;
        } else if (beginStr.startsWith("__")) {
            geneticHolder.type = TYPE_UNDER_LINE;
            geneticHolder.skip = 2;
            return geneticHolder;
        } else if (beginStr.startsWith("--") || beginStr.startsWith("~~")) {
            geneticHolder.type = TYPE_DELETE;
            geneticHolder.skip = 2;
            return geneticHolder;
        } else if (beginStr.startsWith("[")) {
            var hyperHolder = isHyperLink(line, start);
            if (hyperHolder != null) {
                return hyperHolder;
            }
        } else if (beginStr.startsWith("![")) {
            var hyperHolder = isHyperLink(line, start + 1);
            if (hyperHolder != null) {
                hyperHolder.type = TYPE_IMG;
                hyperHolder.skip += 1;
                return hyperHolder;
            }
        }
    }
    geneticHolder.type = TYPE_PARAGRAPH;
    geneticHolder.skip = 0;
    return geneticHolder;
}

function getElementType(start, line) {
    if (start < line.length) {
        var beginStr = line.substr(start, Math.min(line.length, start + 6));
        if (beginStr.startsWith("#")) {
            //heading
            var count = 0;
            while (count < beginStr.length && beginStr.charAt(count) == '#') {
                count++;
            }
            switch (count) {
                case 1:
                    return TYPE_HEADING_1;
                case 2:
                    return TYPE_HEADING_2;
                case 3:
                    return TYPE_HEADING_3;
                case 4:
                    return TYPE_HEADING_4;
                default:
                    return TYPE_HEADING_5;
            }
        } else if (beginStr.startsWith("**")) {
            //bold
            return TYPE_BOLD;
        } else if (beginStr.startsWith("* ") || beginStr.startsWith("- ")) {
            //li
            return TYPE_LI;
        } else if (beginStr.startsWith("'''")) {
            return TYPE_QUOTE;
        }
    }
    return TYPE_PARAGRAPH;
}

function countWhitespace(line) {
    var count = 0;
    while (line.charAt(count) == ' ') {
        count++;
    }
    return count;
}

function isGeneticType(type) {
    return type == TYPE_BOLD || type == TYPE_ITALIC || type == TYPE_IMG || type == TYPE_HYPER || type == TYPE_UNDER_LINE;
}

function isHyperLink(str, start) {
    var stack = [];
    var name = "";
    var url = "";
    var ongoing_str = "";
    var end_index = -1;
    for (var i = start; i < str.length; i++) {
        var c = str.charAt(i);
        if (c == '[') {
            stack.push(c);
        } else if (c == ']') {
            if (stack.length != 1) {
                return null;
            } else {
                stack.push(c);
                name = ongoing_str;
                ongoing_str = "";
            }
        } else if (c == '(') {
            if (stack.length != 2) {
                return null;
            } else {
                stack.push(c);
            }
        } else if (c == ')') {
            if (stack.length != 3 || ongoing_str.length == 0) {
                return null;
            } else {
                stack.push(c);
                url = ongoing_str;
                end_index = i;
                ongoing_str = "";
                break;
            }
        } else {
            ongoing_str += c;
        }
    }
    if (stack.length != 4 || end_index == -1) {
        return null;
    }

    var geneticHolder = new Object();
    geneticHolder.type = TYPE_HYPER;
    geneticHolder.skip = i - start + 1;
    geneticHolder.name = name;
    geneticHolder.url = url;
    return geneticHolder;
}

function is_code_quote(str) {
    if (str.toLowerCase().indexOf("java") != -1) {
        return "java";
    } else if (str.toLowerCase().indexOf("python") != -1) {
        return "python";
    } else if (str.toLowerCase().indexOf("c++") != -1) {
        return "c++";
    } else if (str.toLowerCase().indexOf("html") != -1) {
        return "html";
    } else if (str.toLowerCase().indexOf("javascript") != -1) {
        return "javascript";
    }
    return null;
}


var BaseElement = function (raw_text, depth, whitespace_count) {
    this.type = TYPE_PARAGRAPH;
    this.whitespace_count = whitespace_count;
    this.name = "";
    this.parsed = false;
    this.children = [];
    this.raw_text = raw_text;
    this.display_text = "";
    this.depth = depth;
    this.start_line = -1;
    this.end_line = -1;
};

BaseElement.prototype.append_child = function (childElement) {
    this.children.push(childElement)
};

//parse for paragraph, li
//the sub content can be bold, (possibility italic)
BaseElement.prototype.parse_inside = parse_inside;

BaseElement.prototype.generate_html = function () {
    switch (this.type) {
        case TYPE_PARAGRAPH:
            if (this.display_text.length > 0) {
                return this.display_text;
            } else {
                var p = $("<p>");
                for (var x in this.children) {
                    p.append(this.children[x].generate_html());
                }
            }
            return p;
        case TYPE_LIST:
            var ul = $("<ul>");
            for (var x in this.children) {
                ul.append(this.children[x].generate_html());
            }
            return ul;
        case TYPE_BODY:
            var body = $("<body>");
            for (var x in this.children) {
                body.append(this.children[x].generate_html());
            }
            return body;
        case TYPE_LI:
            var li = $("<li>");
            for (var x in this.children) {
                li.append(this.children[x].generate_html());
            }
            return li;
        case TYPE_QUOTE:
            var quote = $("<blockquote>");
            if (this.children.length == 1 && this.children[0] instanceof CODEElement) {
                quote.attr("class", "code");
            }
            for (var x in this.children) {
                quote.append(this.children[x].generate_html());
            }
            return quote;
        case TYPE_HEADING_1:
        case TYPE_HEADING_2:
        case TYPE_HEADING_3:
        case TYPE_HEADING_4:
        case TYPE_HEADING_5:
            var h = $("<h" + (this.type - 1) + ">");
            h.append(this.display_text);
            return h;
        case TYPE_BOLD:
            var b = $("<b>");
            b.append(this.display_text);
            return b;
        case TYPE_ITALIC:
            var i = $("<i>");
            i.append(this.display_text);
            return i;
        case TYPE_UNDER_LINE:
            var u = $("<p>");
            u.attr("class", "underline")
            u.append(this.display_text);
            return u;
        case TYPE_DELETE:
            var d = $("<p>");
            d.attr("class", "delete")
            d.append(this.display_text);
            return d;
        case TYPE_HYPER:
            var a = $("<a>");
            if (this.url != undefined && this.url.length > 0 && this.url.indexOf("://") == -1) {
                this.url = "http://" + this.url;
            }
            a.attr("href", this.url);
            a.attr("target", "_blank");
            a.attr("class", "link");
            a.append(this.display_text);
            return a;
        case TYPE_IMG:
            var img = $("<img>");
            img.attr("src", this.url);
            return img;
        case TYPE_BR:
            var br = $("<br>");
            return br;
        case TYPE_CODE:
            var pre = document.createElement('pre');
            var code = document.createElement('code');
            code.innerHTML = this.display_text;
            code.className = this.code_type;
            pre.appendChild(code);
            hljs.highlightBlock(pre);
            return pre;
    }
}

BaseElement.prototype.to_string = function element_to_string() {
    return this.name + ", start_line " + this.start_line + ", end line " + this.end_line + ", depth " + this.depth + ", raw text " + this.raw_text;
}


function PElement(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.name = "p";
}
constructSubClass(PElement);

function H1Element(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_HEADING_1;
    this.name = "h1";
}
constructSubClass(H1Element);

function H2Element(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_HEADING_2;
    this.name = "h2";
}
constructSubClass(H2Element);

function H3Element(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_HEADING_3;
    this.name = "h3";
}
constructSubClass(H3Element);

function H4Element(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_HEADING_4;
    this.name = "h4";
}
constructSubClass(H4Element);

function H5Element(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_HEADING_5;
    this.name = "h5";
}
constructSubClass(H5Element);

function QUOTElement(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_QUOTE;
    this.name = "quote";
}
constructSubClass(QUOTElement);

function LIElement(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_LI;
    this.name = "li";
}
constructSubClass(LIElement);

function LISTElement(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_LIST;
    this.name = "list";
}
constructSubClass(LISTElement);

function BODYElement() {
    BaseElement.call(this);
    this.type = TYPE_BODY;
    this.name = "body";
    this.raw_text = "";
}
constructSubClass(BODYElement);

function BOLDElement(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_BOLD;
    this.name = "bold";
}
constructSubClass(BOLDElement);

function ITALICElement(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_ITALIC;
    this.name = "italic";
}
constructSubClass(ITALICElement);

function UNDERLINEElement(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_UNDER_LINE;
    this.name = "underline";
}
constructSubClass(UNDERLINEElement);

function DELETEElement(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_DELETE;
    this.name = "delete";
}
constructSubClass(DELETEElement);

function HYPERTEXTElement(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_HYPER;
    this.name = "hyper";
    this.url = "";
}
constructSubClass(HYPERTEXTElement);

function IMGElement(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_IMG;
    this.name = "img";
    this.url = "";
}
constructSubClass(IMGElement);

function BRElement(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_BR;
    this.name = "br";
    this.url = "";
}
constructSubClass(BRElement);

function CODEElement(raw_text, depth, whitespace_count) {
    BaseElement.call(this, raw_text, depth, whitespace_count);
    this.type = TYPE_CODE;
    this.name = "code";
    this.url = "";
}
constructSubClass(CODEElement);

function constructSubClass(ElementBase) {
    ElementBase.prototype = Object.create(BaseElement.prototype)
    ElementBase.prototype.constructor = ElementBase
}


window.parser = parse;


function log(text) {
    if (log_enable) {
        console.log(text)
    }
}