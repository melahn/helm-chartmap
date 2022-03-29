# This script will generate the examples of helm chart maps included with the helm-chartmap project
#
# It uses relative directories based on the source directory containing the script file so it should
# be executed in that directory or the relative directories should be modified to your preference
jarfile=../../target/helm-chartmap-1.1.0.jar
docdir=../../docs
pumlopt=-DPLANTUML_LIMIT_SIZE=8192

# Update the local charts
helm repo update

# Nuxeo
chart=nuxeo
outdir=$docdir/$chart
java          -jar $jarfile -c "$chart:3.0.3" -o $outdir/"$chart-3.0.3.txt"   
java          -jar $jarfile -c "$chart:3.0.3" -o $outdir/"$chart-3.0.3.json" 
java $pumlopt -jar $jarfile -c "$chart:3.0.3" -o $outdir/"$chart-3.0.3.puml" -g
java          -jar $jarfile -c "$chart:3.0.9" -o $outdir/"$chart-3.0.9.txt"   
java          -jar $jarfile -c "$chart:3.0.9" -o $outdir/"$chart-3.0.9.json" 
java $pumlopt -jar $jarfile -c "$chart:3.0.9" -o $outdir/"$chart-3.0.9.puml" -g
# Alfresco Content Services
chart=alfresco-content-services
outdir=$docdir/$chart
java          -jar $jarfile -c "$chart:3.0.8" -o $outdir/"$chart-3.0.8.txt"  
java          -jar $jarfile -c "$chart:3.0.8" -o $outdir/"$chart-3.0.8.json" 
java $pumlopt -jar $jarfile -c "$chart:3.0.8" -o $outdir/"$chart-3.0.8.puml" -g
# Alfresco DBP
chart=alfresco-dbp
outdir=$docdir/$chart
java          -jar $jarfile -c "$chart:1.5.0" -o $outdir/"$chart-1.5.0.txt"  
java          -jar $jarfile -c "$chart:1.5.0" -o $outdir/"$chart-1.5.0.json" 
java $pumlopt -jar $jarfile -c "$chart:1.5.0" -o $outdir/"$chart-1.5.0.puml" -g
# Identity Service
chart=alfresco-identity-service
outdir=$docdir/$chart
java          -jar $jarfile -c "$chart:1.0.0" -o "$outdir/$chart-1.0.0.txt"  
java          -jar $jarfile -c "$chart:1.0.0" -o "$outdir/$chart-1.0.0.json" 
java $pumlopt -jar $jarfile -c "$chart:1.0.0" -o "$outdir/$chart-1.0.0.puml" -g
java          -jar $jarfile -c "$chart:1.1.0" -o "$outdir/$chart-1.1.0.txt"  
java          -jar $jarfile -c "$chart:1.1.0" -o "$outdir/$chart-1.1.0.json" 
java $pumlopt -jar $jarfile -c "$chart:1.1.0" -o "$outdir/$chart-1.1.0.puml" -g
java          -jar $jarfile -c "$chart:1.1.1" -o "$outdir/$chart-1.1.1.txt"  
java          -jar $jarfile -c "$chart:1.1.1" -o "$outdir/$chart-1.1.1.json" 
java $pumlopt -jar $jarfile -c "$chart:1.1.1" -o "$outdir/$chart-1.1.1.puml" -g
java          -jar $jarfile -c "$chart:2.0.0" -o "$outdir/$chart-2.0.0.txt"  
java          -jar $jarfile -c "$chart:2.0.0" -o "$outdir/$chart-2.0.0.json" 
java $pumlopt -jar $jarfile -c "$chart:2.0.0" -o "$outdir/$chart-2.0.0.puml" -g
java          -jar $jarfile -c "$chart:3.0.0" -o "$outdir/$chart-3.0.0.txt"  
java          -jar $jarfile -c "$chart:3.0.0" -o "$outdir/$chart-3.0.0.json" 
java $pumlopt -jar $jarfile -c "$chart:3.0.0" -o "$outdir/$chart-3.0.0.puml" -g
java          -jar $jarfile -c "$chart:4.0.0" -o "$outdir/$chart-4.0.0.txt"  
java          -jar $jarfile -c "$chart:4.0.0" -o "$outdir/$chart-4.0.0.json" 
java $pumlopt -jar $jarfile -c "$chart:4.0.0" -o "$outdir/$chart-4.0.0.puml" -g
java          -jar $jarfile -c "$chart:5.0.0" -o "$outdir/$chart-5.0.0.txt"  
java          -jar $jarfile -c "$chart:5.0.0" -o "$outdir/$chart-5.0.0.json" 
java $pumlopt -jar $jarfile -c "$chart:5.0.0" -o "$outdir/$chart-5.0.0.puml" -g
java          -jar $jarfile -c "$chart:6.0.0" -o "$outdir/$chart-6.0.0.txt"  
java          -jar $jarfile -c "$chart:6.0.0" -o "$outdir/$chart-6.0.0.json" 
java $pumlopt -jar $jarfile -c "$chart:6.0.0" -o "$outdir/$chart-6.0.0.puml" -g
# Alfresco Process Services
chart=alfresco-process-services
outdir=$docdir/$chart
java          -jar $jarfile -c "$chart:0.2.0" -o "$outdir/$chart-0.2.0.txt"  
java          -jar $jarfile -c "$chart:0.2.0" -o "$outdir/$chart-0.2.0.json" 
java $pumlopt -jar $jarfile -c "$chart:0.2.0" -o "$outdir/$chart-0.2.0.puml" -g
java          -jar $jarfile -c "$chart:0.3.0" -o "$outdir/$chart-0.3.0.txt"  
java          -jar $jarfile -c "$chart:0.3.0" -o "$outdir/$chart-0.3.0.json" 
java $pumlopt -jar $jarfile -c "$chart:0.3.0" -o "$outdir/$chart-0.3.0.puml" -g
# Wordpress
chart=wordpress
outdir=$docdir/$chart
java          -jar $jarfile -c "$chart:8.1.2"   -o "$outdir/$chart-8.1.2.txt"    
java          -jar $jarfile -c "$chart:8.1.2"   -o "$outdir/$chart-8.1.2.json"   
java $pumlopt -jar $jarfile -c "$chart:8.1.2"   -o "$outdir/$chart-8.1.2.puml"   -g
java          -jar $jarfile -c "$chart:10.6.10" -o "$outdir/$chart-10.6.10.txt"  
java          -jar $jarfile -c "$chart:10.6.10" -o "$outdir/$chart-10.6.10.json" 
java $pumlopt -jar $jarfile -c "$chart:10.6.10" -o "$outdir/$chart-10.6.10.puml" -g
java          -jar $jarfile -c "$chart:13.1.4"  -o "$outdir/$chart-13.1.4.txt"   
java          -jar $jarfile -c "$chart:13.1.4"  -o "$outdir/$chart-13.1.4.json"  
java $pumlopt -jar $jarfile -c "$chart:13.1.4"  -o "$outdir/$chart-13.1.4.puml"  -g

echo "Done. Look in $docdir for the generated charts."
