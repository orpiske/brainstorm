package org.brainstorm.cli.command;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.RegistryException;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import org.jboss.logging.Logger;
import picocli.CommandLine;

@CommandLine.Command(name = "acquisition",
        description = "Create a new brainstorm package for an acquisition worker", sortOptions = false)
public class PackageAcquisitionWorker extends PackageWorker {
    private static final Logger LOG = Logger.getLogger(PackageAcquisitionWorker.class);

    private static final String BASE_DIR = "/opt/brainstorm/";
    private static final String ACQUISITION_DIR = BASE_DIR + "/acquisition";
    private static final String CLASSPATH_DIR = BASE_DIR + "/classpath";


    @CommandLine.Option(names = {"--base-image"}, description = "The default base image", defaultValue = "quay.io/bstorm/camel-worker:latest", arity = "0..1")
    protected String baseImage;

    @CommandLine.Option(names = {"--output-image"}, description = "The default base image", defaultValue = "quay.io/bstorm/camel-worker-layered:latest", arity = "0..1")
    protected String outputImage;

    @CommandLine.Option(names = {"--ingestion"}, description = "The ingestion file to use", arity = "0..1")
    private String ingestion;

    @CommandLine.Option(names = {"--artifact"}, description = "Code/binary artifacts to add to the container", arity = "0..*")
    private List<String> artifacts;

    @Override
    public void run() {

        try {
            LOG.debugf("Building based on %s", baseImage);
            final JibContainerBuilder jibContainerBuilder = Jib.from(baseImage)
                    .addLayer(List.of(Paths.get(ingestion)), AbsoluteUnixPath.get(ACQUISITION_DIR));

            if (artifacts != null && !artifacts.isEmpty()) {
                jibContainerBuilder.addLayer(artifacts.stream().map(s -> Paths.get(s)).toList(),
                        AbsoluteUnixPath.get(CLASSPATH_DIR));
            }
            jibContainerBuilder.containerize(Containerizer.to(RegistryImage.named(outputImage).addCredential(username, password)));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (RegistryException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CacheDirectoryCreationException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InvalidImageReferenceException e) {
            throw new RuntimeException(e);
        }

    }
}
