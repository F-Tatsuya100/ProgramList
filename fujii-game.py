#-*- cording: utf-8 -*-

import sys
import datetime
import random
import socket
import time
import netifaces as ni

Win = ['       W','       I','       N','       N','       E','       R','       !']
Lose = ['       L','       O','       S','       E','       R','       !']
Draw = ['       D','       R','       A','       W','       !']

def GetNowTime():
  NowTime = datetime.datetime.now()
  return NowTime

#Server's Side setting
def MakeServer():
  Serversocket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)

  #ip address search
  ni.ifaddresses('eth1')
  ip = ni.ifaddresses('eth1')[2][0]['addr']

  port = raw_input('port number >>')
  print('make room: ip...' + ip + ' room port...' + port)
  Serversocket.bind((ip,int(port)))
  Serversocket.listen(5)
  print('players waiting....')
  (clientsocket,address) = Serversocket.accept()
  print("Connection successful !!")
  print('connected from ' + str(address[0]) + ' port ' + port)
  ServerReadys(clientsocket)

#Client's Side setting
def SearchServer():
  Clientsocket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
  Serverip = raw_input('server,s ip>>')
  Roomport = raw_input('room,s port number>>')
  Clientsocket.connect((Serverip,int(Roomport)))
  ClientReadys(Clientsocket)


#ready to start Game
def ServerReadys(clientsocket):
  print("players waiting...")
  reply = clientsocket.recv(1024)
  if reply == 'OK':
    print("player OK")
    print("Play the Game")
    print("\n-------Server's Turn-------\n")
    while True:
      ReadyS = raw_input("Server's Turn Ready? OK >>")
      if ReadyS == 'OK':
        #server's Turn
        DictTime = random.randint(3,8)
        GS = GetNowTime()
        ResS = PlayGAME(DictTime,GS,clientsocket)
        print("\n-------Client's Turn-------")
        print("waiting client's result...\n")
        ResC = clientsocket.recv(1024)
      break
    print("\n------RESULT------\n\n")
    time.sleep(2)
    print('server  ' + str(ResS))
    print('client  ' + str(ResC))
    hanteiS = WinLose(ResS,ResC,clientsocket)
    time.sleep(2)
    resirast(hanteiS)
    print("")
    quit(clientsocket)


def ClientReadys(Clientsocket):
  while True:
    ReadyU = raw_input("Are you Ready? OK >>")
    if ReadyU == 'OK':
      Clientsocket.send(ReadyU)
      print("\n-------Server's Turn-------")
      print("waiting server's result ...\n")
      ResS = Clientsocket.recv(1024)
      #client's Turn
      print("\n-------Client's Turn-------\n")
      TurnReady = raw_input("Client's Turn Ready? OK>>")
      while True:
        if TurnReady == 'OK':
          DictTime = random.randint(3,8)
          GS = GetNowTime()
          ResC = PlayGAME(DictTime,GS,Clientsocket)
          break
    break
  print("\n------RESULT------\n\n")
  time.sleep(3)
  print('Server  ' + ResS)
  print('client  ' + ResC)
  hanteiC = Clientsocket.recv(1024)
  time.sleep(2)
  resirast(hanteiC)
  print("")
  quit(Clientsocket)


#game start
def PlayGAME(dicttime,GStartTime,Socket):
  print("\n\n")
  print("Ready?")
  while 1:
    now = GetNowTime()
    dist = now - GStartTime
    if dist.total_seconds() >= dicttime:
      Go = GetNowTime()
      push = raw_input('!! >>')
      if push == '':
        P = GetNowTime()
        result = P - Go
        print(result.total_seconds())
        Socket.send(str(result.total_seconds()))
        return str(result.total_seconds())


#solo_play game start
def Solo_play():
  DictTime = random.randint(3,8)
  while 1:
    now = GetNowTime()
    dist = now - GStartTime
    if dist.total_seconds() >= DictTime:
      Go = GetNowTime()
      push = raw_input('!! >>')
      if push == '':
        P = GetNowTime()
        result = P - Go
        print(result.total_seconds())
        break


#winner annaly
def WinLose(s,c,Csocket):
  clie = float(c)
  serv = float(s)
  resl = serv - clie  #server winner
  if resl < 0:
    Csocket.send('2')
    return 1
  elif resl > 0:
    Csocket.send('1')
    return 2
  else:
    Csocket.send('3')
    return 3

#Result Display
def resirast(han):
  print("\n")
  if int(han) == 1:
    for i in range(len(Win)):
      print(Win[i])
  elif int(han) == 2:
    for i in range(len(Lose)):
      print(Lose[i])
  else:
    for i in range(len(Draw)):
      print(Draw[i])
  print("\n")

#end jobs
def quit(sockets):
  print('GAME OVER')
  sockets.shutdown(socket.SHUT_RDWR)
  sockets.close()


#main
#room select
roomode = raw_input('input number 1:make room 2:in to room also:testplay>>')
if roomode == '1':
  MakeServer()

elif roomode == '2':
  SearchServer()

else:
  GStartTime = GetNowTime()
  print("Ready?")
  Solo_play()
