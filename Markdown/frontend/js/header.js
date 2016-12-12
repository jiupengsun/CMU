/**
 * Created by suanmiao on 10/6/16.
 */
$(document).ready(function () {
    var chat = window.chat;
    var uid = window.util.get_uid();
    var nickname = window.util.get_nickname();
    var docid = window.util.QueryString.docid;
    console.log("docid: " + docid + " uid:" + uid);

    $("#header_logout").click(function () {
        $.ajax({
            url: 'backend/user/logout',
            async: false,
            success: function (response) {
                if (window.util.check_response_status(response)) {
                    location.href = "/login.html"
                }
            }
        });
    })

    $("#header_username").text(nickname);
    $("#header_avatar").attr("src", window.util.get_avatar())
    if (chat != undefined) {
        var connection_text = $("#header_connect_status")
        chat.register_connection_callback(connection_callback)

        function connection_callback(status, owner) {
            if (status == STATUS_NONE) {
                connection_text.text("Disconnected");
            } else {
                connection_text.text("Connected");
            }
        }
    }

});

