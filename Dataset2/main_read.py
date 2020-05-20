import read_try
import os
for count in range(1,14,2):
	print("Starting file:")
	print(count)
	read_try.initialize(count)
	print("------------------")
	print("The file:")
	print(count)
	print(" is Ready!!!")
	print("------------------")
