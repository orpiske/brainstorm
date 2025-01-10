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

import static org.brainstorm.cli.command.Constants.CAMEL_SINK_LATEST;

@CommandLine.Command(name = "sink",
        description = "Create a new brainstorm package for an sink worker", sortOptions = false)
public class PackageSinkWorker extends PackageWorker {
    private static final Logger LOG = Logger.getLogger(PackageSinkWorker.class);

    private static final String BASE_DIR = "/opt/brainstorm/";
    private static final String SINK_DIR = BASE_DIR + "/sink";
    private static final String CLASSPATH_DIR = BASE_DIR + "/classpath";


    @CommandLine.Option(names = {"--base-image"}, description = "The default base image", defaultValue = CAMEL_SINK_LATEST, arity = "0..1")
    protected String baseImage;

    @CommandLine.Option(names = {"--output-image"}, description = "The default base image", defaultValue = Constants.CAMEL_SINK_LAYERED_LATEST, arity = "0..1")
    protected String outputImage;

    @CommandLine.Option(names = {"--step"}, description = "The sink file to use", arity = "0..1")
    private String ingestion;

    @CommandLine.Option(names = {"--artifact"}, description = "Code/binary artifacts to add to the container", arity = "0..*")
    private List<String> artifacts;

    @Override
    public void run() {

        try {
            LOG.debugf("Building based on %s", baseImage);
            final JibContainerBuilder jibContainerBuilder = Jib.from(baseImage)
                    .addLayer(List.of(Paths.get(ingestion)), AbsoluteUnixPath.get(SINK_DIR));

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
