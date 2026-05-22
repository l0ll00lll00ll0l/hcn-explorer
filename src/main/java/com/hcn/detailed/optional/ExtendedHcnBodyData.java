package com.hcn.detailed.optional;

import com.hcn.detailed.Hcn;
import lombok.Data;

@Data
public class ExtendedHcnBodyData {
    private Hcn firstGeneratedHcn = null;
    private Hcn firstSuperiorHcn = null;
    private Hcn firstPostProvedNonSuperiorHcn = null;
    private Hcn firstPreProvedNonSuperiorHcn = null;
}
