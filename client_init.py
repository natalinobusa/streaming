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


# jogging with Linda
post('/api/streams',{})
post('/api/streams/1/in/filters',{"resolution":1, "field":"heart", "transform":"max", "group_by":"name"})
post('/api/streams/1/in/filters',{"resolution":1, "field":"heart", "transform":"count", "action": {"url":"https://dashku.com/api/transmission", "params":{"_id" : "54217aeb69e5b36a5b00360b", "apiKey" : "0902f285-8824-4745-8f8b-81f24985fa1b"}}})

# access logs with nasa
post('/api/streams',{})
post('/api/streams/2/in/filters',{"resolution":1, "field":"status", "transform":"count", "group_by":"url"})
post('/api/streams/2/in/filters',{"resolution":1, "field":"status", "transform":"count", "group_by":"code"})
post('/api/streams/2/in/filters',{"resolution":1, "field":"status", "transform":"count"})


