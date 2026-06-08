@echo off
cd /d "%~dp0"
ngrok.exe http --domain=reconvene-luridness-curdle.ngrok-free.dev 3000
