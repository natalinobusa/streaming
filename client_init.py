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
post('/api/streams/1/in/filters',{"resolution":1, "field":"heart", "transform":"count"})

# access logs with nasa
post('/api/streams',{})
post('/api/streams/2/in/filters',{"resolution":1, "field":"status", "transform":"count", "group_by":"url"})
post('/api/streams/2/in/filters',{"resolution":1, "field":"status", "transform":"count", "group_by":"code"})
post('/api/streams/2/in/filters',{"resolution":1, "field":"status", "transform":"count"})


