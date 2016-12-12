/**
 * Created by suanmiao on 05/11/2016.
 */
const STATUS_NONE = 1;
const STATUS_CONNECTED = 2;
var uid = window.util.get_uid();
var nickname = window.util.get_nickname();
var avatar = window.util.get_avatar();
var docid = window.util.QueryString.docid;
console.log("docid: " + docid + " uid:" + uid);

var ws_status = STATUS_NONE;
var ws;

var editor_callback_list = []
var access_callback_list = []
var connection_callback_list = []
console.log("session id:" + window.util.get_session_id());

var chat_item_mine_template = $(".chat_item_mine_template")
var chat_item_other_template = $(".chat_item_other_template")

function init_ws() {
    ws = new ReconnectingWebSocket("ws://" + window.location.hostname + ":8000/doc/" + docid + "?session_key=" + window.util.get_session_id())
    ws.timeoutInterval = 3000;
    ws.maxReconnectInterval = 50000;
    ws.reconnectDecay = 1.5;
    ws.onopen = function () {
        console.log("socket connected")
        ws_status = STATUS_CONNECTED;
        broadcast_connection_status(ws_status, uid)
    };
    ws.onmessage = function (event) {
        var packet = JSON.parse(event.data);
        if (util.check_packet_status(packet)) {
            var data = packet.data;
            if (data.action.indexOf("chat") != -1) {
                bind_chat_data(packet);
            } else if (data.action.indexOf("doc") != -1) {
                broadcast_edit_message(packet)
            } else if (data.action.indexOf("access") != -1) {
                broadcast_access_message(packet)
            }
        }else{
            ws.close()
            var flag = parseInt(packet.flag);
            switch(flag){
                case 120:
                    //user not login
                    bootbox.alert("You has not login, click the ensure button to login", function(){
                        location.href = "/login.html"
                    });
                    break;
                default:
                    //document error
                    bootbox.alert("The document does not exist or you have no permission", function(){
                        location.href = "/profile.html"
                    });
                    break;
            }
        }
    };
    ws.onclose = function () {
        console.log("socket closed");
        ws_status = STATUS_NONE
        broadcast_connection_status(ws_status, uid)
    }
}

function broadcast_edit_message(msg) {
    for (var i in editor_callback_list) {
        editor_callback_list[i](msg)
    }
}

function broadcast_access_message(msg) {
    for (var i in access_callback_list) {
        access_callback_list[i](msg)
    }
}

function broadcast_connection_status(status, owner) {
    for (var i in access_callback_list) {
        connection_callback_list[i](status, owner)
    }
}

function register_connection_callback(callback) {
    connection_callback_list.push(callback)
    broadcast_connection_status(ws_status, uid)
}

function register_editor_callback(callback) {
    editor_callback_list.push(callback)
}

function register_access_callback(callback) {
    access_callback_list.push(callback)
}

function send_editor_message(opt) {
    console.log("status:" + ws_status);
    if (ws_status == STATUS_CONNECTED) {
        var packet = new Object();
        if (opt.operation == "add" || opt.operation == "delete" || opt.operation == "update") {
            var data = new Object();
            data.action = "doc/" + opt.operation;
            data.uid = uid + "";
            data.docid = docid + "";
            data.start = opt.startLocation;
            data.end = opt.endLocation;
            data.content = opt.content;
            packet.data = data;
            ws.send(JSON.stringify(packet));
        }
    } else {
        console.log("not connected");
    }
}

function send_chat_message(content) {
    var packet = new Object();
    var data = new Object();
    data.action = "chat";
    data.uid = uid + "";
    data.docid = docid + "";
    data.nickname = nickname + "";
    data.content = content;
    packet.data = data;

    if (ws_status == STATUS_CONNECTED) {
        ws.send(JSON.stringify(packet));
    } else {
        console.log("ws not connected");
    }
}

var bind_chat_data = function (packet) {
    var item;
    if (packet.data.uid == uid) {
        item = chat_item_mine_template.clone();
        item.removeClass("chat_item_mine_template");
        item.addClass("chat_item_mine");
        item.find(".chat_item_avatar").attr("src", avatar);
        item.find(".chat_item_avatar_min").attr("src", avatar);
    } else {
        item = chat_item_other_template.clone();
        item.removeClass("chat_item_other_template");
        item.addClass("chat_item_other");
        item.find(".chat_item_avatar").attr("src", packet.data.avatar);
        item.find(".chat_item_avatar_other").attr("src", packet.data.avatar);
    }
    if (packet.data.action == "chat/join") {
        item.find(".chat_item_content").text(packet.data.nickname + " join the chat");
    } else {
        item.find(".chat_item_content").text(packet.data.content);
    }

    var list = $("#chat_content_list");
    list.append(item)
    list.scrollTop(10000)
};

$("#chat_text_input").keyup(function (event) {
    if (event.keyCode == 13) {
        var content = $("#chat_text_input").val()
        console.log("var:" + content);
        if (content.length > 0) {
            send_chat_message(content);
            $("#chat_text_input").val("");
        }
    }
});

$("#chat_button_send").click(function () {
    var content = $("#chat_text_input").val()
    console.log("var:" + content);
    if (content.length > 0) {
        send_chat_message(content);
        $("#chat_text_input").val("");
    }
});

window.chat = new Object();
window.chat.init_ws = init_ws
window.chat.send_editor_message = send_editor_message
window.chat.register_editor_callback = register_editor_callback
window.chat.register_access_callback = register_access_callback
window.chat.register_connection_callback = register_connection_callback
