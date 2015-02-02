#!/bin/bash
scp -r frontend/ root@bacon:/opt/bacon/
scp bacon-server.yml root@bacon:/opt/bacon/bacon.yml
scp warmup.sh root@bacon:/opt/bacon/warmup.sh