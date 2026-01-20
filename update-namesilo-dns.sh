#!/bin/bash
# Updates Namesilo DNS with current ECS Fargate public IP
set -euo pipefail

# Configuration
API_KEY="15fc56289aa5698b5d7c41ce"
DOMAIN="jcarl.net"
SUBDOMAIN="project"
FULL_HOST="project.jcarl.net"
TTL="3600"
API_BASE="https://www.namesilo.com/api"

log() { echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"; }

# 1. Get ECS Task Public IP
log "Getting ECS Task Public IP..."

# Get the task ARN
TASK_ARN=$(aws ecs list-tasks --cluster music-library-cluster1 --service-name music-library-service --query 'taskArns[0]' --output text)

if [[ "$TASK_ARN" == "None" || -z "$TASK_ARN" ]]; then
    log "ERROR: No running tasks found"
    exit 1
fi

# Get the ENI ID from the task
ENI_ID=$(aws ecs describe-tasks --cluster music-library-cluster1 --tasks "$TASK_ARN" --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`].value' --output text)

# Get the public IP from the ENI
PUBLIC_IP=$(aws ec2 describe-network-interfaces --network-interface-ids "$ENI_ID" --query 'NetworkInterfaces[0].Association.PublicIp' --output text)

log "ECS Task Public IP: $PUBLIC_IP"

# 2. Find ALL existing records for this subdomain
log "Checking for existing records..."
RECORDS=$(curl -s "${API_BASE}/dnsListRecords?version=1&type=xml&key=${API_KEY}&domain=${DOMAIN}")

# PARSING FIX: 
# 1. Use sed to replace the closing tag with a newline, putting each record on its own line.
# 2. Grep for the specific host (project.jcarl.net).
# 3. Extract the record_id.
IDS=$(echo "$RECORDS" | sed 's/<\/resource_record>/\n/g' | grep ">${FULL_HOST}<" | grep -o "<record_id>[^<]*" | cut -d'>' -f2)

# 3. Delete ALL found records
if [[ -n "$IDS" ]]; then
    for id in $IDS; do
        log "Deleting old record ID: $id"
        # We allow this to fail silently in case the ID is weird, but we log the attempt
        curl -s "${API_BASE}/dnsDeleteRecord?version=1&type=xml&key=${API_KEY}&domain=${DOMAIN}&rrid=${id}" > /dev/null || true
    done
else
    log "No existing records found (or parsing failed)."
fi

# 4. Create a FRESH Record
log "Creating new A Record pointing to $PUBLIC_IP..."
ADD_URL="${API_BASE}/dnsAddRecord?version=1&type=xml&key=${API_KEY}&domain=${DOMAIN}&rrtype=A&rrhost=${SUBDOMAIN}&rrvalue=${PUBLIC_IP}&rrttl=${TTL}"

RESPONSE=$(curl -s "$ADD_URL")

if echo "$RESPONSE" | grep -q "<code>300</code>"; then
    log "SUCCESS: DNS record created successfully!"
else
    log "ERROR: Failed to create record."
    log "Response: $RESPONSE"
    exit 1
fi