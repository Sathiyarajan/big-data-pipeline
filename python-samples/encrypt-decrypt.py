# initializing string  
str = "Sathya123$"

# encoding string  
str_enc = str.encode('base64', 'strict') 

# printing the encoded string 
print "The encoded string in base64 format is : ", 
print str_enc 

# printing the original decoded string  
print "The decoded string is : ", 
print str_enc.decode('base64', 'strict') 
