var util = new Object({
        check_response_status: function (response) {
            if (response == null || response == undefined || response.flag == undefined || response.flag == null || parseInt(response.flag) != 0) {
                return false;
            }
            return true;
        },

        check_packet_status: function (packet) {
            if (packet == null || packet == undefined || (packet.flag != null && packet.flag != undefined && parseInt(packet.flag) != 0)) {
                return false;
            }
            return true;
        },
        extract_error_message: function (response) {
            return JSON.stringify(response.message);
        },

        get_data_from_response: function (response) {
            return response.data;
        },

        save_uid: function (uid) {
            localStorage.setItem("uid", uid);
        },

        save_nickname: function (nickname) {
            localStorage.setItem("nickname", nickname);
        },

        save_avatar: function (avatar) {
            localStorage.setItem("avatar", avatar);
        },

        get_uid: function () {
            return localStorage.getItem("uid");
        },

        get_avatar: function () {
            return localStorage.getItem("avatar");
        },

        get_nickname: function () {
            return localStorage.getItem("nickname");
        },

        get_session_id: function () {
            var jsId = document.cookie.match(/sessionid=([^;]+)/);
            return jsId[1];
        },
        bind_input_event: function (id, callback) {
            $('#' + id).bind('input propertychange', callback);
        },
        enable_tab: function (id) {
            var el = document.getElementById(id);
            el.onkeydown = function (e) {
                if (e.keyCode === 9) { // tab was pressed

                    // get caret position/selection
                    var val = this.value,
                        start = this.selectionStart,
                        end = this.selectionEnd;

                    // set textarea value to: text before caret + tab + text after caret
                    this.value = val.substring(0, start) + '\t' + val.substring(end);

                    // put caret at right position again
                    this.selectionStart = this.selectionEnd = start + 1;

                    // prevent the focus lose
                    return false;

                }
            };
        },
        set_cursor_position: function (elem, caretPos) {
            if (elem != null) {
                if (elem.createTextRange) {
                    var range = elem.createTextRange();
                    range.move('character', caretPos);
                    range.select();
                }
                else {
                    if (elem.selectionStart) {
                        elem.focus();
                        elem.setSelectionRange(caretPos, caretPos);
                    }
                    else
                        elem.focus();
                }
            }
        },
        bind_cursor_position: function (callback) {
            document.onselectionchange = callback;
        },
        register_login_callback: function (callback) {
            $.ajax({
                url: 'backend/user/islogin',
                async: false,
                success: function (response) {
                    if (!window.util.check_response_status(response)) {
                        location.href = "/login.html"
                    } else {
                        var uid = window.util.get_data_from_response(response).uid;
                        var nickname = window.util.get_data_from_response(response).nickname;
                        var avatar = window.util.get_data_from_response(response).avatar;
                        var user = Object();
                        user.uid = uid;
                        user.nickname = nickname;
                        user.avatar = avatar;
                        window.util.save_uid(uid);
                        window.util.save_nickname(nickname);
                        window.util.save_avatar(avatar);
                        callback(user);
                    }
                }
            });

        },
        QueryString: function () {
            // This function is anonymous, is executed immediately and
            // the return value is assigned to QueryString!
            var query_string = {};
            var query = window.location.search.substring(1);
            var vars = query.split("&");
            for (var i = 0; i < vars.length; i++) {
                var pair = vars[i].split("=");
                // If first entry with this name
                if (typeof query_string[pair[0]] === "undefined") {
                    query_string[pair[0]] = decodeURIComponent(pair[1]);
                    // If second entry with this name
                } else if (typeof query_string[pair[0]] === "string") {
                    var arr = [query_string[pair[0]], decodeURIComponent(pair[1])];
                    query_string[pair[0]] = arr;
                    // If third or later entry with this name
                } else {
                    query_string[pair[0]].push(decodeURIComponent(pair[1]));
                }
            }
            return query_string;
        }(),
    })
    ;

window.util = util
