import repo_Mining
import os
'''
The main execution of repo_mining.py
Call the function for each mini dataset.
'''
for count in range(1,36,1):
	print("Starting file:")
	print(count)
	repo_Mining.initialize(count)
	print("------------------")
	print("The file:")
	print(count)
	print(" is Ready!!!")
	print("------------------")
