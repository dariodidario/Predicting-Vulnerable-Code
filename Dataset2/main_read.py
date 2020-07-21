import read_try
import os
for count in range(1,36,1):
	print("Starting file:")
	print(count)
	read_try.initialize(count)
	print("------------------")
	print("The file:")
	print(count)
	print(" is Ready!!!")
	print("------------------")
