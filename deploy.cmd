call gcloud functions deploy rtf-to-html-demo --gen2 --entry-point=demo.RtfToHtml --runtime=java21 --region=europe-west10 --source=./target --trigger-http --allow-unauthenticated

pause

rem see https://cloud.google.com/functions/docs/create-deploy-http-java?hl=de