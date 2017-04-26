package kr.ac.kaist.activity.injection;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.MethodReference;
import kr.ac.kaist.activity.injection.analysis.IntentAnalysis;
import kr.ac.kaist.activity.injection.appinfo.ActivityInfoExtractor;
import kr.ac.kaist.activity.injection.callgraph.CHACallGraphBuiler;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by leesh on 21/02/2017.
 */
public class Shell {

    private static final boolean DEBUG = true;
    private static final String destDir = "affinities";
    private static String appName;

    public static void main(String[] args){
        String property = args[0];
        String apk = args[1];
        appName = apk.substring(0, apk.lastIndexOf("."));

        try {
//            ActivityInfoExtractor extractor = new ActivityInfoExtractor(apk);
//            Set<ActivityInfoExtractor.ActivityInfo> infos = extractor.extract();
//            for(ActivityInfoExtractor.ActivityInfo info : infos){
//                System.out.println(info);
//            }
//
//            boolean isAnalyzable = isAnalyzable(infos, extractor.getAppPackageName());
//            System.err.println("#Analyzable? " + isAnalyzable);
//
//            if(!DEBUG){
//                if(!isAnalyzable){
//                    removeFile(apk);
//                }else{
//                    moveToDest(apk, extractor.getDecompDir());
//                }
//                extractor.removeDecompDir();
//            }

            CHACallGraphBuiler builder = new CHACallGraphBuiler(property, apk);

            CallGraph cg = builder.buildCallGraph();
            IntentAnalysis ia = new IntentAnalysis(cg);
            ia.analyze();
//            CallingComponentAnalysis cca = new CallingComponentAnalysis(cg);
//            for(CallingComponentAnalysis.ComponentCallingContext cc : cca.getCallingContexts()){
//                System.out.println(cc);
//            }

//        } catch (ParserConfigurationException e) {
//            e.printStackTrace();
//        } catch (SAXException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
        } catch (ClassHierarchyException e) {
            e.printStackTrace();
        }
    }

    private static Set<CGNode> findStartActivityNode(CallGraph cg, MethodReference mr){
        Set<CGNode> targetNodes = new HashSet<>();

        //get startActivity nodes of ContextWrapper
        Iterator<CGNode> iNodes = cg.getNodes(mr).iterator();
        while(iNodes.hasNext())
            targetNodes.add(iNodes.next());

        //recursively get startActivity nodes of subclasses of ContextWrapper
        for(IClass sub : cg.getClassHierarchy().computeSubClasses(mr.getDeclaringClass())){

            MethodReference subMr = MethodReference.findOrCreate(sub.getReference(), mr.getSelector());
            Iterator<CGNode> iSubNodes = cg.getNodes(subMr).iterator();
            while(iSubNodes.hasNext())
                targetNodes.add(iSubNodes.next());
        }

        return targetNodes;
    }

    private static boolean isAnalyzable(Set<ActivityInfoExtractor.ActivityInfo> infos, String packageName){
        for(ActivityInfoExtractor.ActivityInfo info : infos){
            if(!info.getTaskAffinity().equals(packageName)) {
                if(!info.getTaskAffinity().equals(""))
                    return true;
            }
        }
        return false;
    }

    private static void removeFile(String apk){
        File f = new File(apk);
        f.delete();
    }

    private static void moveToDest(String apk, String decompDir){
        File dest = new File(destDir);
        if(!dest.exists())
            dest.mkdir();

        ProcessBuilder pb = null;
        try {
            pb = new ProcessBuilder("mv", apk, dest.getCanonicalPath());
            pb.start();
            File ttt = new File(decompDir + File.separator + "AndroidManifest.xml");
            System.out.println(decompDir + File.separator + "AndroidManifest.xml (" + ttt.exists() + ")");

            System.out.println(dest.getCanonicalPath() + File.separator + appName + "_AndroidManifest.xml");
            pb = new ProcessBuilder("mv", decompDir + File.separator + "AndroidManifest.xml", dest.getCanonicalPath() + File.separator + appName + "_AndroidManifest.xml");
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
