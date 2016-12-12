from django.test import TestCase, Client
from django.contrib.auth.models import User
from usermanage.models import *
import json

# Create your tests here.


class UserExtendTest(TestCase):

  client = Client()

  def test_signup(self):
    request_object = {
      'username': 'sam',
      'nickname': 'sam',
      'email': 'samparly@gmail.com',
      'password': '123456',
      'confirm_password': '123456',
    }

    self.assertTrue(User.objects.all().count() == 0)
    response = self.client.post('/user/signup/', request_object)
    self.assertEqual(response.status_code, 200)
    self.assertTrue(User.objects.all().count() == 1)
    response_object = json.loads(response.content)
    self.assertTrue(response_object)
    self.assertTrue(response_object["flag"] == 0)
    self.assertTrue(response_object["data"]["uid"])


  def test_signin(self):
    self.test_signup()
    request_object = {
      'username': 'sam',
      'password': '123456',
    }
    response = self.client.post("/user/signin/", request_object)
    self.assertEqual(response.status_code, 200)
    response_object = json.loads(response.content)
    self.assertTrue(response_object)
    self.assertTrue(response_object["flag"] == 0)
    self.assertTrue(response_object["data"]["uid"])

  def test_logout(self):
    self.test_signin()
    response = self.client.get("/user/logout/")
    self.assertEqual(response.status_code, 200)
    print(response.content)