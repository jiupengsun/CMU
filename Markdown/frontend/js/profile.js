/**
 * Created by raylu on 11/6/16.
 */

function on_login(user) {
    const STATUS_NONE = 1;
    const STATUS_CONNECTED = 2;
    

    var my_uid = window.util.get_uid();
    var ws_status = STATUS_NONE;
    var ws;
    console.log("session id:" + window.util.get_session_id());

    var curr_edit_docid;
    var shared_list_item_template = $("#shared_list_item_template")
    var owned_list_item_template = $("#owned_list_item_template")
    var notification_invite_msg_template = $("#notification-invite-msg-template")
    var notification_reply_msg_template = $("#notification-reply-msg-template")
    

    list_documents()


    var count = $("#notification_menu").children().length
    if(count != 0){
        $("#notification_count").text(count);
    }else{
         $("#notification_count").text('');
    }
    console.log("initial count",count)

    $("#btn-form-new-doc").click(function () {
        var form = $("#new-doc-form")
        var doc_title = form.find("#doc-title-input").val()
        var shared_users = form.find("#shared-users-input").val()
        console.log("CREATE doc title: " + doc_title)
        console.log("CREATE shared users: " + shared_users)
        var shared_users_array = shared_users.split(",")

        create_document(doc_title, shared_users_array)
    })

    $('body').on('click', 'a.btn-delete', function (event) {
        delete_document(event)
    })

    $('body').on('click', 'a.btn-edit', function (event) {
        var btn = event.target
        var list = $(btn).parent().parent().parent()
        console.log("edit btn parent list: " + list.attr("id"))

        var doc_id = list.attr("id")
        console.log("edit doc id:" + doc_id)

        curr_edit_docid = doc_id
    })

    $("#btn-form-edit-doc").click(function () {
        var form = $("#edit-doc-form")
        var doc_title = form.find("#doc-title-edit-input").val()
        var shared_users = form.find("#shared-users-edit-input").val()
        console.log("EDIT doc title: " + doc_title)
        console.log("EDIT shared users: " + shared_users)
        var shared_users_array = shared_users.split(",")

        edit_owned_document(doc_title, shared_users_array)
    })

    // $('body').on('click', 'button.btn-notification-accept', function (event) {
    //     send_accept_msg(event)
    // })

    // $('body').on('click', 'button.btn-notification-decline', function (event) {
    //     send_decline_msg(event)
    // })

    // $('body').on('click', 'button.btn-notification-read', function (event) {
    //     send_read_msg(event)
    // })

    init_ws();

    function getCookie(name) {
        var cookieValue = null;
        if (document.cookie && document.cookie !== '') {
            var cookies = document.cookie.split(';');
            for (var i = 0; i < cookies.length; i++) {
                var cookie = jQuery.trim(cookies[i]);
                // Does this cookie string begin with the name we want?
                if (cookie.substring(0, name.length + 1) === (name + '=')) {
                    cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                    break;
                }
            }
        }
        return cookieValue;
    }
    var csrftoken = getCookie('csrftoken');

    function create_document(doc_title, shared_users_array) {
        var success_callback = function (response) {
            console.log("response", response)

            if (window.util.check_response_status(response)) {
                var docid = window.util.get_data_from_response(response).docid
                console.log("create document with id " + docid)
                if (docid != null && shared_users_array.length > 0){
                    send_inivitation_msg(docid, shared_users_array)
                }
                location.href = "/profile.html"
            } else {
                alert("Create document failure: " + window.util.extract_error_message(response));
            }
        };
        //var title = prompt("Enter the document name: ", "My first document");

        formData = new FormData()
        formData.append("title", doc_title)
        formData.append("shared_user", JSON.stringify(shared_users_array))
        $.ajax({
            url: "/backend/doc/create",
            type: "POST",
            data: formData,
            contentType: false,
            processData: false,
            async: true,
            cache: false,
            success: success_callback,
        });
    }

    function delete_document(event) {
        console.log("enter delete document");
        var success_callback = function (response) {
            console.log("response", response);

            if (window.util.check_response_status(response)) {
                alert("Delete document success")
                delete_doc_dom(doc_id);
                //location.href = "/profile.html";
            } else {
                alert("Delete document failure: " + window.util.extract_error_message(response))
            }
        };

        var btn = event.target
        console.log("event by btn: " + btn.id)
        var list = $(btn).parent().parent().parent()
        console.log("delete btn parent list: " + list.attr("id"))

        var doc_id = list.attr("id")
        console.log("delete doc id:" + doc_id)

        formData = new FormData()
        formData.append("docid", doc_id)
        $.ajax({
            url: "/backend/doc/delete",
            type: "POST",
            data: formData,
            contentType: false,
            processData: false,
            async: true,
            cache: false,
            success: success_callback,
        });
    }

    function delete_doc_dom(docid){
        $("#documents-list-container").find("#"+docid).remove();
    }

    function edit_owned_document(doc_title, shared_users_array) {
        console.log("enter edit document");
        var success_callback = function (response) {
            console.log("response", response);

            if (window.util.check_response_status(response)) {
                alert("Edit document success")
                 if (curr_edit_docid != null && shared_users_array.length > 0){
                    send_inivitation_msg(curr_edit_docid, shared_users_array)
                }
                location.href = "/profile.html";
            } else {
                alert("Edit document failure: " + window.util.extract_error_message(response))
            }
        };

        formData = new FormData()
        formData.append("docid", curr_edit_docid)
        formData.append("title", doc_title)
        formData.append("shared_user", JSON.stringify(shared_users_array))

        $.ajax({
            url: "/backend/doc/edit",
            type: "POST",
            data: formData,
            contentType: false,
            processData: false,
            async: true,
            cache: false,
            success: success_callback,
        });
    }

    function list_documents() {
        var success_callback = function (response) {
            console.log("enter list documents")

            if (window.util.check_response_status(response)) {

                var data = window.util.get_data_from_response(response)
                var shared_docs = []
                var owned_docs = []
                shared_docs = shared_docs.concat(data.shared)
                owned_docs = owned_docs.concat(data.owned)
                var shared_len = shared_docs.length
                var owned_len = owned_docs.length

                for (var i = 0; i < shared_len; i++) {
                    var shared_doc_id = shared_docs[i].docid
                    var shared_doc_title = shared_docs[i].title
                    //var shared_doc_time = shared_docs[i].ctime
                    var href = "/edit.html?docid=" + shared_doc_id

                    var new_item = shared_list_item_template.clone()
                    new_item.attr("id", shared_doc_id)
                    new_item.removeClass()
                    new_item.addClass("list-doc")
                    new_item.find(".doc-name").text(shared_doc_title)
                    new_item.find(".doc-name").attr("href", href)
                    //new_item.find(".doc-time").text(shared_doc_time)

                    var list = $("#shared-documents-list")
                    list.append(new_item)
                }

                for (var i = 0; i < owned_len; i++) {
                    var owned_doc_id = owned_docs[i].docid
                    var owned_doc_title = owned_docs[i].title
                    //var owned_doc_time = owned_docs[i].ctime
                    var href = "/edit.html?docid=" + owned_doc_id

                    var new_item = owned_list_item_template.clone()
                    new_item.attr("id", owned_doc_id)
                    new_item.removeClass()
                    new_item.addClass("list-doc")
                    new_item.find(".doc-name").text(owned_doc_title)
                    new_item.find(".doc-name").attr("href", href)
                    //new_item.find(".doc-time").text(owned_doc_time)

                    var list = $("#owned-documents-list")
                    list.append(new_item)
                }

            } else {
                alert("List document failure: " + window.util.extract_error_message(response));
            }
        };

        formData = new FormData()
        $.ajax({
            url: "/backend/doc/list",
            type: "POST",
            data: formData,
            contentType: false,
            processData: false,
            async: true,
            cache: false,
            success: success_callback,
        });
    }

    function init_ws() {
        ws = new ReconnectingWebSocket("ws://" + window.location.hostname + ":8000/notify/" + "?session_key=" + window.util.get_session_id())
        ws.debug = true;
        ws.timeoutInterval = 3000;
        ws.maxReconnectInterval = 50000;
        ws.reconnectDecay = 1.5;

        ws.onopen = function () {
            console.log("socket connected")
            ws_status = STATUS_CONNECTED;
        };
        ws.onmessage = function (event) {
            var packet = JSON.parse(event.data);
            if (packet.data != undefined){
                var data = packet.data
                var data_len = data.length           
                for (var i=0; i<data_len; i++){
                    var cur_data = data[i]

                    if (cur_data.other_user == my_uid.toString()){
                        console.log("enter delete if statement: " + cur_data)
                        delete_notification(cur_data)
                    }
                    else{
                        console.log("flag" + packet.data.flag + "data length: " + data_len);
                        var type = cur_data.type
                        var nid = cur_data.nid
                        console.log(i + "th data type: " + type + " nid: " + nid);
                        //"0"-invitation
                        if (type == "0"){
                            get_invitation_msg(cur_data)
                        }
                        //"1"-accept
                        else if (type == "1"){
                            get_accept_msg(cur_data)
                        }
                        //"2"-decline
                        else if (type == "2"){
                            get_decline_msg(cur_data)
                        }
                    }
                    
                }
                //console.log("packet:" + JSON.stringify(event.data));
            }
        };
        ws.onclose = function () {
            console.log("socket closed");
            ws_status = STATUS_NONE
        }
    }

    function delete_notification(data){
        console.log("enter delete notification")
        var nid = data.nid

        $("#" + nid).remove();
        
        if (data.message_type == "0"){
            $("#shared-documents-list").empty();
            $("#owned-documents-list").empty();
            list_documents();
        }

        var count = $("#notification_menu").children().length
        if(count != 0){
            $("#notification_count").text(count);
        }else{
            $("#notification_count").text('');
        }
        console.log("delete count",count)
    }

    function send_inivitation_msg(docid, shared_users_array){
        console.log("enter send invitation msg")
        var len = shared_users_array.length
        for (var i=0; i<len; i++){
            var packet = new Object();
            var data = new Object();

            data.message_type = "0";
            data.docid = docid + "";
            data.other_user = shared_users_array[i];
            packet.data = data;

            console.log("send invitation data: " + data)

            if (ws_status == STATUS_CONNECTED) {
                ws.send(JSON.stringify(packet));
            } else {
                console.log("ws not connected");
            }
        }
    }

    function send_accept_msg(event){

        var btn = event.target
        console.log("send accept msg by btn: " + btn.id)
        var msg = $(btn).parent()
        var nid = msg.attr("id")
        var other_user = $(msg).find("#other-user").text().replace(/[\r\n]/g, '');
        console.log("accept msg list: " + msg.attr("id"))
        console.log("accept msg list: " + msg.attr("id"))

        var packet = new Object();
        var data = new Object();

        data.message_type = "1"
        data.other_user = other_user + "";
        data.nid = nid

        packet.data = data

        console.log("send accept msg data: " + data)

        if (ws_status == STATUS_CONNECTED) {
            ws.send(JSON.stringify(packet));
        } else {
            console.log("ws not connected");
        }

        //$('#notification_holder').load("/profile.html" +  ' #notification_holder');
        //location.href = "/profile.html"
    }

    function send_decline_msg(event){

        var btn = event.target
        console.log("send decline msg by btn: " + btn.id)
        var msg = $(btn).parent()
        var nid = msg.attr("id")
        var other_user = $(msg).find("#other-user").text().replace(/[\r\n]/g, '');

        console.log("accept msg list: " + msg.attr("id"))
        console.log("the other user: " + other_user)

        var packet = new Object();
        var data = new Object();

        data.message_type = "2"
        data.other_user = other_user + "";
        data.nid = nid

        packet.data = data

        console.log("send decline msg data: " + data)
        
        if (ws_status == STATUS_CONNECTED) {
            ws.send(JSON.stringify(packet));
        } else {
            console.log("ws not connected");
        }

        //$('#notification_holder').load("/profile.html" +  ' #notification_holder');

        //location.href = "/profile.html"
    }

    function send_read_msg(event){

        var btn = event.target
        console.log("send got-it msg by btn: " + btn.id)
        var msg = $(btn).parent()
        var nid = msg.attr("id")
        console.log("accept msg list: " + msg.attr("id"))

        var packet = new Object();
        var data = new Object();

        data.message_type = "3"
        data.nid = nid

        packet.data = data

        console.log("send read msg packet: " + packet)
        
        if (ws_status == STATUS_CONNECTED) {
            ws.send(JSON.stringify(packet));
        } else {
            console.log("ws not connected");
        }

        //$('#notification_holder').load("/profile.html" +  ' #notification_holder');
        //location.href = "/profile.html"
    }

    function get_invitation_msg(data){

        console.log("get invitation msg: " + data)

        var username = data.user_send.nickname
        var uid = data.user_send.uid
        var time = data.time
        var doc_title = data.doc.title
        var nid = data.nid

        var content = username.concat(" invites you to edit Document: \"", doc_title).concat("\"")

        var new_msg = notification_invite_msg_template.clone()
        new_msg.attr("id", nid)
        new_msg.removeClass()
        new_msg.addClass("notification-invite-msg")
        new_msg.find(".notification-msg-text").text(content)
        new_msg.find(".notification-msg-time").text(time)
        new_msg.find("#other-user").text(uid)
        new_msg.find(".btn-notification-accept").click(function (event) {
            send_accept_msg(event)
        })

        new_msg.find(".btn-notification-decline").click(function (event) {
            send_decline_msg(event)
        })

        var list = $("#notification_menu")
        list.prepend(new_msg)

        var count = $("#notification_menu").children().length
        if(count != 0){
            $("#notification_count").text(count);
        }else{
            $("#notification_count").text('');
        }
        console.log("get invitation count",count)
    }

    function get_accept_msg(data){
        console.log("get accept msg: " + data)
    

        var username = data.user_send.nickname
        var time = data.time
        var doc_title = data.doc.title
        var nid = data.nid

        var content = username.concat(" has accepted your invitation to edit Document: \"", doc_title).concat("\"")

        var new_msg = notification_reply_msg_template.clone()
        new_msg.attr("id", nid)
        new_msg.removeClass()
        new_msg.addClass("notification-reply-msg")
        new_msg.find(".notification-msg-text").text(content)
        new_msg.find(".notification-msg-time").text(time)
        new_msg.find(".btn-notification-read").click(function (event) {
            send_read_msg(event)
        })

        var list = $("#notification_menu")
        
        list.prepend(new_msg) 

        var count = $("#notification_menu").children().length
        if(count != 0){
            $("#notification_count").text(count);
        }else{
            $("#notification_count").text('');
        }
        console.log("get accept count",count)
    }

    function get_decline_msg(data){
        console.log("get decline msg: " + data)
        // NOTIFICATION_COUNT = NOTIFICATION_COUNT + 1;
       

        var username = data.user_send.nickname
        var time = data.time
        var doc_title = data.doc.title
        var nid = data.nid

        var content = username.concat(" has declined your invitation to edit Document: \"", doc_title).concat("\"")

        var new_msg = notification_reply_msg_template.clone()
        new_msg.attr("id", nid)
        new_msg.removeClass()
        new_msg.addClass("notification-reply-msg")
        new_msg.find(".notification-msg-text").text(content)
        new_msg.find(".notification-msg-time").text(time)
        new_msg.find(".btn-notification-read").click(function (event) {
            send_read_msg(event)
        })

        var list = $("#notification_menu")
        list.prepend(new_msg) 

        var count = $("#notification_menu").children().length
        if(count != 0){
            $("#notification_count").text(count);
        }else{
            $("#notification_count").text('');
        }
        console.log("get decline count",count)
    }

    function list_count(list){
        var count = list.length
        return count
    }

}

$(document).ready(function () {
    window.util.register_login_callback(function (user) {
        on_login(user);
    });
})


