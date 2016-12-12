/**
 * Created by suanmiao on 9/18/16.
 */
var avatar = window.util.get_avatar();
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

var login_success = function () {
    location.href = "/profile.html"
}

$(document).ready(function () {

    $.ajax({
        url: 'backend/user/islogin',
        async: false,
        success: function (response) {
            if (window.util.check_response_status(response)) {
                login_success();
            } else {
                console.log("not login")
            }
        }
    });

    $("#login-tab-signup").click(function () {
        $("#login-tab-signup").addClass("active");
        $("#login-tab-login").removeClass("active");
        var list = $(".login-form");
        for (var i = list.length - 1; i >= 0; i--) {
            list[i].style.display = "none";
        }
        list = $(".signup-form");
        for (var i = list.length - 1; i >= 0; i--) {
            list[i].style.display = "block";
        }
    })

    $("#login-tab-login").click(function () {
        $("#login-tab-login").addClass("active");
        $("#login-tab-signup").removeClass("active");
        var list = $(".login-form");
        for (var i = list.length - 1; i >= 0; i--) {
            list[i].style.display = "block";
        }
        list = document.getElementsByClassName("signup-form");
        for (var i = list.length - 1; i >= 0; i--) {
            list[i].style.display = "none";
        }
    })
    $("#login-tab-login").trigger("click");

    $(".login-form-confirm").click(function (event) {
        event.preventDefault()
        var formData = new FormData();
        var username = $(".login-form-username")[0].value;
        var password = $(".login-form-password")[0].value;
        var check_input = function (input_name, input) {
            if (input.length == 0) {
                alert("Please input correct " + input_name)
                return true;
            } else if (input.length > 20) {
                alert("The " + input_name + " you input is too long")
                return true;
            }
            return false;
        }
        if (check_input("username", username) || check_input("password", password)) {
            return;
        }
        formData.append('username', username);
        formData.append('password', password);

        var success_callback = function (response) {
            if (window.util.check_response_status(response)) {
                var uid = window.util.get_data_from_response(response).uid;
                var nickname = window.util.get_data_from_response(response).nickname;
                var avatar = window.util.get_data_from_response(response).avatar;
                window.util.save_uid(uid);
                window.util.save_nickname(nickname);
                window.util.save_avatar(avatar);
                login_success()
            } else {
                bootbox.alert("Login failure: " + window.util.extract_error_message(response));
            }
        };

        $.ajax({
            url: "/backend/user/signin",
            type: "POST",
            data: formData,
            contentType: false,
            processData: false,
            async: true,
            cache: false,
            success: success_callback,
        });
    });


    $(".signup-form-confirm").click(function (event) {
        event.preventDefault()
        var username = $(".signup-form-username")[0].value;
        var email = $(".signup-form-email")[0].value;
        var password = $(".signup-form-password")[0].value;
        var password_confirm = $(".signup-form-password-confirm")[0].value;
        var nickname = $(".signup-form-nickname")[0].value;
        var avatar = $(".signup-form-avatar")[0].files[0];

        var check_input = function (input_name, input) {
            if (input.length == 0) {
                alert("Please input correct " + input_name)
                return true;
            } else if (input.length > 20) {
                alert("The " + input_name + " you input is too long")
                return true;
            }
            return false;
        }
        if (check_input("username", username) || check_input("nickname", nickname) || check_input("password", password)) {
            event.preventDefault()
            return false;
        }
        var input_img = $(".signup-form-avatar")[0].files[0];
        if (input_img != undefined && !(input_img.name.toString().toLowerCase().endsWith(".png") ||
            input_img.name.toString().toLowerCase().endsWith(".jpg") ||
            input_img.name.toString().toLowerCase().endsWith(".jpeg"))) {
            bootbox.alert("The file you selected is not valid image !")
            event.preventDefault()
            return false;
        }
        var formData = new FormData();

        formData.append('username', username);
        formData.append('email', email);
        formData.append('password', password);
        formData.append('confirm_password', password_confirm);
        formData.append('nickname', nickname);
        formData.append('avatar', avatar);

        var success_callback = function (response) {
            if (window.util.check_response_status(response)) {
                bootbox.alert("Signup Success!");
                location.href = "/login.html"
            } else {
                bootbox.alert("Signup failure: " + window.util.extract_error_message(response));
            }
        };

        $.ajax({
            url: "/backend/user/signup",
            type: "POST",
            data: formData,
            contentType: false,
            processData: false,
            async: true,
            cache: false,
            success: success_callback,
        });

    });
    return false;
})
