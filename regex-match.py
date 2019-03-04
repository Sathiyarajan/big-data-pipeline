#https://stackoverflow.com/questions/8888567/match-a-line-with-multiple-regex-using-python
import re
stri="hello hella hellb hellc helld"
regexList = ["hella", "hellb", "hellc"]
gotMatch = False
for regex in regexList:
    s = re.search(regex,stri)
    if s:
        gotMatch = True
        break
if gotMatch:
    print("got match")

regexes= 'quick', 'brown', 'fox'
combinedRegex = re.compile('|'.join('(?:{0})'.format(x) for x in regexes))

lines = 'The quick brown fox jumps over the lazy dog', 'Lorem ipsum dolor sit amet', 'The lazy dog jumps over the fox'

for line in lines:
    print(combinedRegex.findall(line))
