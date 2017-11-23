
package uk.ac.eb.pride.cluster.reanalysis.processsteps;

import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import uk.ac.eb.pride.cluster.reanalysis.control.engine.ProcessingEngine;
import uk.ac.eb.pride.cluster.reanalysis.control.util.JarLookupService;
import uk.ac.eb.pride.cluster.reanalysis.control.util.ZipUtils;
import uk.ac.eb.pride.cluster.reanalysis.model.enums.AllowedDenovoGUIParams;
import uk.ac.eb.pride.cluster.reanalysis.model.exception.PladipusProcessingException;
import uk.ac.eb.pride.cluster.reanalysis.model.exception.UnspecifiedPladipusException;
import uk.ac.eb.pride.cluster.reanalysis.model.processing.ProcessingStep;
import uk.ac.eb.pride.cluster.reanalysis.util.PladipusFileDownloadingService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Kenneth Verheggen
 */
public class DenovoGUIStep extends ProcessingStep {

    private static final Logger LOGGER = Logger.getLogger(DenovoGUIStep.class);
    private static final File temp_deNovoGUI_output = new File(System.getProperty("user.home") + "/.compomics/pladipus/temp/DeNovoGUI/result");

    public DenovoGUIStep() {

    }

    private List<String> constructArguments() throws IOException, UnspecifiedPladipusException {
        File deNovoGUIJar = getJar();
        ArrayList<String> cmdArgs = new ArrayList<>();
        cmdArgs.add("java");
        cmdArgs.add("-cp");
        cmdArgs.add(deNovoGUIJar.getAbsolutePath());
        cmdArgs.add("com.compomics.denovogui.cmd.DeNovoCLI");
        for (AllowedDenovoGUIParams aParameter : AllowedDenovoGUIParams.values()) {
            if (parameters.containsKey(aParameter.getId())) {
                cmdArgs.add("-" + aParameter.getId());
                cmdArgs.add(parameters.get(aParameter.getId()));
            } else if (aParameter.isMandatory()) {
                throw new IllegalArgumentException("Missing mandatory parameter : " + aParameter.id);
            }
        }
        return cmdArgs;
    }

    @Override
    public boolean doAction() throws UnspecifiedPladipusException,PladipusProcessingException {
        LOGGER.info("Running " + this.getClass().getName());
        File parameterFile = new File(parameters.get("id_params"));
        LOGGER.info("Updating parameters...");
        try {
            SearchParameters identificationParameters = SearchParameters.getIdentificationParameters(parameterFile);
            //fix the location
            SearchParameters.saveIdentificationParameters(identificationParameters, parameterFile);
            if (temp_deNovoGUI_output.exists()) {
                temp_deNovoGUI_output.delete();
            }
            temp_deNovoGUI_output.mkdirs();

            LOGGER.info("Starting searchGUI...");
            //use this variable if you'd run following this classs
            File real_outputFolder = new File(parameters.get("output_folder"));
            parameters.put("output_folder", temp_deNovoGUI_output.getAbsolutePath());
            new ProcessingEngine().startProcess(getJar(), constructArguments());
            //storing intermediate results
            LOGGER.info("Storing results in " + real_outputFolder);
            FileUtils.copyDirectory(temp_deNovoGUI_output, real_outputFolder);
            //in case of future peptideShaker searches :
            parameters.put("identification_files", temp_deNovoGUI_output.getAbsolutePath());
        } catch (IOException|ClassNotFoundException ioe){
            UnspecifiedPladipusException ex = new UnspecifiedPladipusException("sumting went rong");
            ex.addSuppressed(ioe);
            throw ex;
        }
        return true;
    }

    public File getJar() throws IOException,UnspecifiedPladipusException {
        //check if this is possible in another way...
        File toolFolder = new File(System.getProperties().getProperty("user.home") + "/.compomics/pladipus/tools");
        toolFolder.mkdirs();
        //check if searchGUI already exists?
        File temp = new File(toolFolder, "DeNovoGUI");
        if (!temp.exists()) {
            File searchGUIFile = PladipusFileDownloadingService.downloadFile(parameters.get("DeNovoGUI"), toolFolder);
            if (searchGUIFile.getName().endsWith(".zip")) {
                ZipUtils.unzipArchive(searchGUIFile, temp);
            }
        }
        return JarLookupService.lookupFile("DeNovoGUI-.*.jar", temp);
    }

    public boolean aVersionExistsLocal() {
        //TODO insert installer code here in case searchGUI was not included????
        return true;
    }

    @Override
    public String getDescription() {
        return "Running DenovoGUI";
    }

}
