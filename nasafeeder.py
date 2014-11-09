#!/usr/bin/python

import sys
import json
import requests
import time

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


f = open ('nasa.access.sample.log', 'r')

count =0
while(1):
  for line in f:
    count += 1
    d = line.split()
    payload = { 'time': d[0], 'status': int(d[2]), 'url':d[1], 'code':d[2], 'size':d[3] }
    post('/api/streams/2/in', payload )
    time.sleep(0.01)
    if (count % 100) ==0 :
      get('/api/streams/2/in/filtered_by/1/out')
      get('/api/streams/2/in/filtered_by/2/out')
      get('/api/streams/2/in/filtered_by/3/out')
  f.seek(0, 0)

