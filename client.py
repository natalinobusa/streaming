#!/usr/bin/python

import json
import requests
import time
import random 

api = 'http://localhost:8888'
headers = {'content-type': 'application/json'}

def post(path, payload="") :
  r = requests.post(api+path, data=json.dumps(payload), headers=headers)
  if (r.text):
    print json.dumps(json.loads(r.text), indent=2)

def get(path) :
  r = requests.get(api+path, headers=headers)
  if (r.text):
    print json.dumps(json.loads(r.text), indent=2)

def delete(path) :
  r = requests.delete(api+path, headers=headers)
  if (r.text):
    print json.dumps(json.loads(r.text), indent=2)


girls = ("Lara","Wendy", "Cindy", "Linda", "Naomi")

while(1):
  for i in range(100):
    rate = round(random.gauss(random.gauss(90,20), 20), 2)
    post('/api/streams/1/in',{ "heart":rate, "name":random.choice(girls) } )
    time.sleep(0.01)
  get('/api/streams/1/in/filtered_by/1/out')
  get('/api/streams/1/in/filtered_by/2/out')

