# open.lpd

A LPD protocol implementation for server and client according to RFC 1179 - "Line Printer Daemon Protocol".

## Contents

a LPD server protocol implementation so you can write your own server (open.lpd.server.LpdServerProtocol)
a LPD client protocol implementation so you can write your own client (open.lpd.client.LpdClientProtocol)
a ready to go LPD server (open.lpd.server.impl.LpdServer)
a ready to go LPD client (open.lpd.client.impl.LpdClient)

## LPD server protocol implementation

Use the LpdServerProtocol class to implement your own LPD server. The LpdServerProtocol uses the 
IPrintJobQueue interface as a queue back end to handle print jobs.

## LPD client protocol implementation

Use the LpdClientProtocol class to implement your own LPD client. 

## LPD server

A ready to go LPD server that uses a file based queue implementation. Print jobs are stored as sub folders of
queue folders.

Queues folder structure example:
```
  queues/  
    AFP/ ...................... queue folder of queue "AFP"
    PDF/ ...................... queue folder of queue "PDF"
      1406576720765-1 ......... print job folder (=print job name)
        cfA000localhost ....... LPD control file
        dfA000localhost.PDF ... data file
    RAW/ ...................... queue folder of queue "RAW"
    TXT/ ...................... queue folder of queue "TXT"
      1406576408562-0/ ........ print job folder (=print job name)
        cfA000localhost ....... LPD control file
        dfA000localhost.TXT ... data file
```    
Usage:
```    
  --host <hostname/ip> ... hostname/ip to listen for client connections (default "0.0.0.0")
  --port <port> ... port to listen on (default 515)
  --script <cmd> ... cmd to run for each print job, use $1 for queue name and $2 for print job folder (default "queue.sh $1 $2")
  --queuefolder <folder> ... queue folder that receives print job folders (default "queues")
  --socketbacklogsize <size> ... socket backlog size (default 100)
  --clientConnectionThreads <count> ... max number of concurrent client threads (default 8)
```    

Examples:
```    
  start server on 0.0.0.0:515 and run cmd wscript.exe work/scripts/queue.vbs //nologo $1 \"$2\" on every print job 
    --host 0.0.0.0 --port 515 --script "wscript.exe work/scripts/queue.vbs //nologo $1 \"$2\""
```    

## LPD client

A ready to go LPD client (aka "lpr") that can send files and standard LPD commands to a LPD server.

Usage:
```    
  --cmd <print|send|state|lstate|remove> ... LPD command to perform
  --queue <name> ... name of the print queue
  --file <path> ... path of file to send
  --agent <name> ... user agent name
```    
Examples:
```
  send file docs/rfc1179.txt to print queue TXT on print server my.print.host:515 on behalf of user test
    --cmd send --queue TXT --file docs/rfc1179.txt --agent test --host my.print.host --port 515
```
 