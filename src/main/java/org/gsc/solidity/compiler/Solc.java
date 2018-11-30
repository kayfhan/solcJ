/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.gsc.solidity.compiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

import lombok.extern.slf4j.Slf4j;
import org.gsc.config.SystemProperties;

/**
 * Created by Anton Nashatyrev on 03.03.2016.
 */

@Slf4j
public class Solc {

    private File solc = null;
    private String solcVersion = null;

    Solc(SystemProperties config) {
        try {
            init(config);
        } catch (IOException e) {
            throw new RuntimeException("Can't init solc compiler: ", e);
        }
    }

    private void init(SystemProperties config) throws IOException {
        if (null != config.getSolcVersion()){
            solcVersion = "0.4.25";
        }
        solcVersion = config.getSolcVersion();

        if (config != null && config.getCustomSolcPath() != null) {
            solc = new File(config.getCustomSolcPath());
            if (!solc.canExecute()) {
                throw new RuntimeException(String.format(
                        "Solidity compiler from config solc.path: %s is not a valid executable",
                        config.getCustomSolcPath()
                ));
            }
        } else {
            initBundled();
        }
    }

    private void initBundled() throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), "solc");
        tmpDir.mkdirs();

        InputStream is = getClass().getResourceAsStream("/native/" + getOS() + "/v" + solcVersion + "/file.list");
        System.out.println("/native/" + getOS() + "/file.list");
        try (Scanner scanner = new Scanner(is)) {
            while (scanner.hasNext()) {
                String s = scanner.next();
                File targetFile = new File(tmpDir, s);
                InputStream fis = getClass().getResourceAsStream("/native/" + getOS() + "/v" + solcVersion + "/" + s);
                System.out.println("/native/" + getOS() + "/v" + solcVersion + "/" + s);
                Files.copy(fis, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                if (solc == null) {
                    // first file in the list denotes executable
                    solc = targetFile;
                    solc.setExecutable(true);
                }
                targetFile.deleteOnExit();
            }
        }
    }

    private String getOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "win";
        } else if (osName.contains("linux")) {
            return "linux";
        } else if (osName.contains("mac")) {
            return "mac";
        } else {
            throw new RuntimeException("Can't find solc compiler: unrecognized OS: " + osName);
        }
    }

    public File getExecutable() {
        return solc;
    }
}
