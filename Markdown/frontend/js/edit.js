/**
 * Created by suanmiao on 10/6/16.
 */
function on_login(user) {
    var chat = window.chat;
    var uid = user.uid;
    var nickname = user.nickname;
    var avatar = user.avatar;
    var docid = window.util.QueryString.docid;
    console.log("docid: " + docid + " uid:" + uid);
    var title = "";
    var rawText = undefined;
    var editorElement = $("#editor");
    var displayElement = $(".preview");
    var editorDocumentElement = document.getElementById("editor")
    var cursor_position = 0;

    $("#chat_dialog").draggable();

    // listen selection change event and update cursor position
    window.util.bind_cursor_position(function () {
        cursor_position = getCaret(editorDocumentElement)
    });
    window.util.enable_tab("editor")
    window.util.bind_input_event('editor', function () {
        re_parse(editorElement.val(), true);
    });

    var re_parse = function (text, local_change) {
        rawText = text;
        editorElement.val(text);
        if (rawText == undefined) {
            return;
        }
        var html = window.parse(rawText)

        displayElement.html(html);
        $(".link").on("click", function (event) {
            var url = $(this).attr("href");
            if (url != undefined && url == "http://reset") {
                event.preventDefault();
                re_parse("", true);
                prevText = "";
                displayElement.html("")
                return false;
            }
            return true;
        });

        if (docid == undefined || docid == null) {
            return;
        }
        //only local change will be send to server, if this is the rebase action from the server
        // we should not send it to the server
        if (local_change) {
            var diff_msg = generateDiff(rawText);
            if (diff_msg != null) {
                chat.send_editor_message(diff_msg);
            }
        }
        prevText = rawText;
    }

    if (docid == undefined || docid == null) {
        return;
    }

    var prevText = ""
    const OPT_NONE = 0;
    const OPT_ADD = 1;
    const OPT_DELETE = 2;
    const OPT_UPDATE = 3;

    chat.init_ws();
    chat.register_editor_callback(editor_message_callback);
    chat.register_access_callback(access_message_callback);

    function editor_message_callback(packet) {
        var action = packet.data.action;
        if (action != undefined) {
            if (action.indexOf("init") != -1) {
                title = packet.data.title;
                updateTitle(title)
                re_parse(packet.data.content, false);
            } else if (packet.data != undefined && packet.data.uid != uid) {
                rebase(packet)
            }
        }
    }

    function access_message_callback(packet) {
        var action = packet.data.action;
        if (action != undefined) {
            if (action.indexOf("add") != -1) {

            } else if (action.indexOf("delete") != -1) {

            }
        }
    }

    function rebase(packet) {
        var prev_cursor_position = cursor_position;
        var data = packet.data;
        var startLocation = data.start;
        var endLocation = data.end;
        var content = data.content;
        if (startLocation > prevText.length) {
            startLocation = prevText.length
        }
        var operation = data.action;
        if (operation.indexOf("add") != -1) {
            var text = prevText.substring(0, startLocation) + content + prevText.substring(endLocation, prevText.length);
            re_parse(text, false);
        } else if (operation.indexOf("delete") != -1) {
            var text = prevText.substring(0, startLocation) + prevText.substring(endLocation, prevText.length);
            re_parse(text, false);
        } else if (operation.indexOf("update") != -1) {
            var text = prevText.substring(0, startLocation) + content + prevText.substring(endLocation, prevText.length);
            re_parse(text, false);
        }
        if (endLocation <= prev_cursor_position) {
            //when current editing influence the user's cursor
            prev_cursor_position += (content.length - (endLocation - startLocation))
            window.util.set_cursor_position(editorDocumentElement, prev_cursor_position);
        }
    }

    function getCaret(node) {
        if (node.selectionStart) {
            return node.selectionStart;
        } else if (!document.selection) {
            return 0;
        }

        var c = "\001",
            sel = document.selection.createRange(),
            dul = sel.duplicate(),
            len = 0;

        dul.moveToElementText(node);
        sel.text = c;
        len = dul.text.indexOf(c);
        sel.moveStart('character', -1);
        sel.text = "";
        return len;
    }

    function updateTitle(text) {
        if (text == undefined) {
            return;
        }
        $("#preview_title").text(text);
    }

    function generateDiff(rawText) {
        var operation = OPT_NONE;
        var content;
        var startLocation = -1;
        var endLocation = -1;
        if (prevText.length == 0 && rawText.length != 0) {
            operation = OPT_ADD;
            startLocation = 0;
            endLocation = 0;
            content = rawText;
        } else if (prevText.length != 0) {
            var l = 0;
            var r = 0;
            while (l < prevText.length && l + r < Math.min(prevText.length, rawText.length) && l < rawText.length && prevText.charAt(l) == rawText.charAt(l)) {
                l++;
            }
            while (r < prevText.length && l + r < Math.min(prevText.length, rawText.length) && r < rawText.length && prevText.charAt(prevText.length - 1 - r) == rawText.charAt(rawText.length - 1 - r)) {
                r++;
            }
            if (l + r == prevText.length) {
                operation = OPT_ADD;
                startLocation = l;
                endLocation = prevText.length - r;
                content = rawText.substring(l, rawText.length - r);
            } else if (l + r == rawText.length) {
                operation = OPT_DELETE;
                startLocation = l;
                endLocation = prevText.length - r;
                content = "";
            } else {
                operation = OPT_UPDATE;
                startLocation = l;
                endLocation = prevText.length - r;
                content = rawText.substring(l, rawText.length - r);
            }
        }

        prevText = rawText;
        if (operation != OPT_NONE) {
            var opt = new Object();
            if (operation == OPT_ADD) {
                opt.operation = "add";
            } else if (operation == OPT_DELETE) {
                opt.operation = "delete";
            } else if (operation == OPT_UPDATE) {
                opt.operation = "update";
            }
            opt.startLocation = startLocation + "";
            opt.endLocation = endLocation + "";
            opt.content = content;
            return opt;
        } else {
            return null;
        }
    }
}

$(document).ready(function () {
    window.util.register_login_callback(function (user) {
        on_login(user);
    });
});

