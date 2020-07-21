import os

cwd = os.getcwd()
csvfile = open('dataset2.csv', 'r').readlines()
filename = 1
if "Dataset_Divided" not in os.listdir():
	os.mkdir("Dataset_Divided")
os.chdir(cwd +"/Dataset_Divided")
for i in range(len(csvfile)):
	if i % 50 == 0:
		open(str(filename) + '.csv', 'w+').writelines(csvfile[i:i+50])
		filename += 1