csvfile = open('dataset2.csv', 'r').readlines()
filename = 1
for i in range(len(csvfile)):
	if i % 50 == 0:
		open(str(filename) + '.csv', 'w+').writelines(csvfile[i:i+50])
		filename += 1