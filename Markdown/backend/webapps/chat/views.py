import random
import string
from django.db import transaction
from django.shortcuts import render, redirect


def home(request):
    if request.method == 'GET':
      return render(request, 'index.html')