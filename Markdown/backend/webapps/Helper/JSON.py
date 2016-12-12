
CONTEXT = {
  'flag': "000",
  'data': {

  },
  'message':{
  }
}

# user signup
USER_SIGNUP = {
  'username': "",
  'nickname': "",
  'email': "",
  'password': "",
  'confirm_password': ""
}

# user signin
USER_SIGNIN = {
  'username': "",
  'password': ""
}

# user edit info
USER_EDIT = {

}

USER = {
  'uid': '',
  'nickname': '',
  'avatar': '',
}

#
ADD_DOC = {
  'docid': ""
}

GET_DOC = {
  'docs': []
}

DOCUMENT = {
  'docid': "",
  'title': "",
  'owner': "", #uid
  'abstract': "",
  'ctime': "",
  'content': "",
  'ltime': "",
  'shared': [], # current shared users
}

DOCUMENT_SIMPLE = {
  'docid': "",
  'title': "",
  'owner': "", #uid
  'ctime': "",
  'ltime': "",
  'shared': [], # current shared users
}

DOCUMENT_LIST = {
  "owned":[],
  "shared":[],
}

CHAT = {
  'uid': '',
  'nickname': '',
  'message': ''
}

WELCOME_MESSAGE = {
  'action': 'chat/join',
  'uid':'',
  'nickname':'',
  'content':'',
}

NOTIFICATION = {
  "user_send": {},
  "time": "",
  "type": 0,
  "doc": {},
  "nid": 0
}

NOTIFICATION_RESPONSE = {
  "message_type": '',
  "other_user": '',
  "nid": ''
}
