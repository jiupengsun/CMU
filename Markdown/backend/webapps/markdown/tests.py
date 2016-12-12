import json

from django.test import TestCase, Client

# Create your tests here.

class MarkdownTest(TestCase):

  client = Client()

  def signup_and_signin(self):
    signup_request_object = {
      'username': 'sam',
      'nickname': 'sam',
      'email': 'sam@gmail.com',
      'password': '123456',
      'confirm_password': '123456',
    }
    response = self.client.post("/user/signup/", signup_request_object)
    self.assertTrue(response.status_code, 200)

    signup_request_object = {
      'username': 'samparly',
      'nickname': 'jiupeng',
      'email': 'samparly@gmail.com',
      'password': '123456',
      'confirm_password': '123456',
    }
    response = self.client.post("/user/signup/", signup_request_object)
    self.assertTrue(response.status_code, 200)

    signup_request_object = {
      'username': 'test',
      'nickname': 'test',
      'email': 'test@gmail.com',
      'password': '123456',
      'confirm_password': '123456',
    }
    response = self.client.post("/user/signup/", signup_request_object)
    self.assertTrue(response.status_code, 200)

    signin_request_object = {
      'username': 'sam',
      'password': '123456',
    }
    response = self.client.post("/user/signin/", signin_request_object)
    self.assertTrue(response.status_code, 200)

  # test add new document
  def test_operate(self):
    # register and login
    self.signup_and_signin()
    create_request_object = {
      "title": "New document",
      "shared_user": [
        "samparly", "test"
      ],
    }

    # create a new doc
    response = self.client.post("/doc/create", create_request_object)
    self.assertTrue(response.status_code, 200)
    response_object = json.loads(response.content)
    self.assertTrue(response_object["flag"] == 0)
    print(response_object)
    docid = response_object["data"]["docid"]
    self.assertTrue(docid)

    # retrieve doc list
    response = self.client.post("/doc/list")
    self.assertTrue(response.status_code, 200)
    response_object = json.loads(response.content)
    print(response_object)

    # delete doc
    response = self.client.post("/doc/delete", {
      "docid": docid
    })
    self.assertTrue(response.status_code, 200)
    self.assertTrue(response.status_code, 200)

    # retrieve doc list
    response = self.client.post("/doc/list")
    self.assertTrue(response.status_code, 200)
    response_object = json.loads(response.content)
    print(response_object)




