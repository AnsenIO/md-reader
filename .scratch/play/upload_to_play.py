#!/usr/bin/env python3
"""
SquadShelf Play upload: takes the release AAB + a Play service-account JSON, uploads to the
internal track via the Android Publisher API (edits flow). Run: python3 upload_to_play.py <aab> <sa_json>
Requires: pip install google-api-python-client google-auth (or uv pip).
"""
import sys, json

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

AAB = sys.argv[1]
SA = sys.argv[2]
PACKAGE = "com.mdreader.app"

scopes = ["https://www.googleapis.com/auth/androidpublisher"]
creds = service_account.Credentials.from_service_account_file(SA, scopes=scopes)
service = build("androidpublisher", "v3", credentials=creds)
edits = service.edits()

edit = edits.insert(body={}, packageName=PACKAGE).execute()
edit_id = edit["id"]
print("edit id:", edit_id)

print("uploading bundle...")
bundle = edits.bundles().upload(
    packageName=PACKAGE,
    editId=edit_id,
    media_body=MediaFileUpload(AAB, mimetype="application/octet-stream"),
).execute()
version_code = bundle["versionCode"]
print("uploaded versionCode:", version_code)

# Track metadata: release notes live in the track resource.
track_body = {
    "releases": [{
        "name": "0.1-beta (internal)",
        "versionCodes": [str(version_code)],
        "status": "completed",
        "releaseNotes": [{"language": "en-US", "text": "v0.1 beta — initial internal release. Markdown reader: open .md from any file manager, browse Downloads, offline rendering."}],
    }],
}
edits.tracks().update(packageName=PACKAGE, editId=edit_id, trackId="internal", body=track_body).execute()
print("track updated")

edits.commit(packageName=PACKAGE, editId=edit_id).execute()
print("committed.")
print("NEXT: go to Play Console → Release → Internal → release details, fill store listing (use .scratch/play/listing.md + assets), publish.")
