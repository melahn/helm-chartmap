# This script will generate the examples of helm chart maps included with the helm-chartmap project
#
# It uses relative directories based on the source directory containing the script file so it should
# be executed in that directory or the relative directories should be modified to your preference
jarfile=../jar/helm-chartmap-1.0.3-SNAPSHOT.jar
helmdir=/Users/melahn/.helm
docdir=../../docs
pumlopt=-DPLANTUML_LIMIT_SIZE=8192

# Update the local charts
helm repo update

# Alfresco Content Services
chart=alfresco-content-services
outdir=$docdir/$chart
java          -jar $jarfile -c "$chart:3.0.8" -o $outdir/"$chart-3.0.8.txt"  -d $helmdir
java          -jar $jarfile -c "$chart:3.0.8" -o $outdir/"$chart-3.0.8.json" -d $helmdir
java $pumlopt -jar $jarfile -c "$chart:3.0.8" -o $outdir/"$chart-3.0.8.puml" -d $helmdir -g
# Alfresco DBP
chart=alfresco-dbp
outdir=$docdir/$chart
java          -jar $jarfile -c "$chart:1.5.0" -o $outdir/"$chart-1.5.0.txt"  -d $helmdir
java          -jar $jarfile -c "$chart:1.5.0" -o $outdir/"$chart-1.5.0.json" -d $helmdir
java $pumlopt -jar $jarfile -c "$chart:1.5.0" -o $outdir/"$chart-1.5.0.puml" -d $helmdir -g
# Identity Service
chart=alfresco-identity-service
outdir=$docdir/$chart
java          -jar $jarfile -c "$chart:1.0.0" -o "$outdir/$chart-1.0.0.txt"  -d $helmdir
java          -jar $jarfile -c "$chart:1.0.0" -o "$outdir/$chart-1.0.0.json" -d $helmdir
java $pumlopt -jar $jarfile -c "$chart:1.0.0" -o "$outdir/$chart-1.0.0.puml" -d $helmdir -g
java          -jar $jarfile -c "$chart:1.1.0" -o "$outdir/$chart-1.1.0.txt"  -d $helmdir
java          -jar $jarfile -c "$chart:1.1.0" -o "$outdir/$chart-1.1.0.json" -d $helmdir
java $pumlopt -jar $jarfile -c "$chart:1.1.0" -o "$outdir/$chart-1.1.0.puml" -d $helmdir -g
java          -jar $jarfile -c "$chart:1.1.1" -o "$outdir/$chart-1.1.1.txt"  -d $helmdir
java          -jar $jarfile -c "$chart:1.1.1" -o "$outdir/$chart-1.1.1.json" -d $helmdir
java $pumlopt -jar $jarfile -c "$chart:1.1.1" -o "$outdir/$chart-1.1.1.puml" -d $helmdir -g
java          -jar $jarfile -c "$chart:2.0.0" -o "$outdir/$chart-2.0.0.txt"  -d $helmdir
java          -jar $jarfile -c "$chart:2.0.0" -o "$outdir/$chart-2.0.0.json" -d $helmdir
java $pumlopt -jar $jarfile -c "$chart:2.0.0" -o "$outdir/$chart-2.0.0.puml" -d $helmdir -g
# Alfresco Process Services
chart=alfresco-process-services
outdir=$docdir/$chart
java          -jar $jarfile -c "$chart:0.2.0" -o "$outdir/$chart-0.2.0.txt"  -d $helmdir
java          -jar $jarfile -c "$chart:0.2.0" -o "$outdir/$chart-0.2.0.json" -d $helmdir
java $pumlopt -jar $jarfile -c "$chart:0.2.0" -o "$outdir/$chart-0.2.0.puml" -d $helmdir -g
# Wordpress
chart=wordpress
outdir=$docdir/$chart
java          -jar $jarfile -c "$chart:8.1.2" -o "$outdir/$chart-8.1.2.txt"  -d $helmdir
java          -jar $jarfile -c "$chart:8.1.2" -o "$outdir/$chart-8.1.2.json" -d $helmdir
java $pumlopt -jar $jarfile -c "$chart:8.1.2" -o "$outdir/$chart-8.1.2.puml" -d $helmdir -g
java          -jar $jarfile -c "$chart:10.6.10" -o "$outdir/$chart-10.6.10.txt"  -d $helmdir
java          -jar $jarfile -c "$chart:10.6.10" -o "$outdir/$chart-10.6.10.json" -d $helmdir
java $pumlopt -jar $jarfile -c "$chart:10.6.10" -o "$outdir/$chart-10.6.10.puml" -d $helmdir -g
