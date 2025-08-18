#!/bin/bash
for (( i=0; i<1050; i=i+1 ))
  do
    curl -X POST http://localhost:8080/api/v1/auth/login \
          -H "Content-Type: application/json" \
          -d '{"email": "user1@ticketon.site", "password":
        "password123"}' >> res.txt
done