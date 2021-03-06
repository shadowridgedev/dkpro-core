/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.dkpro.core.textnormalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.transform.alignment.AlignedString;
import de.tudarmstadt.ukp.dkpro.core.api.transform.type.SofaChangeAnnotation;
import de.tudarmstadt.ukp.dkpro.core.textnormalizer.internal.AnnotationComparator;
import de.tudarmstadt.ukp.dkpro.core.textnormalizer.util.NormalizationUtils;

/**
 * Base class for normalizers
 *
 */
@TypeCapability(
        inputs = {
                "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token"},
        outputs = {
                "de.tudarmstadt.ukp.dkpro.core.api.transform.type.SofaChangeAnnotation"})
@Deprecated
public abstract class Normalizer_ImplBase
    extends JCasAnnotator_ImplBase
{

    /**
     * A map, where a token position maps to a list of SofaChangeAnnotations that should be applied
     * for that token
     * 
     * @param jcas
     *            a CAS.
     * @return the mapping.
     */
    protected abstract Map<Integer, List<SofaChangeAnnotation>> createSofaChangesMap(JCas jcas);

    /**
     * A map showing which token should be kept and which should be replaced. "true" indicates:
     * "replace with changed version"
     * 
     * @param jcas
     *            a CAS.
     * @param as
     *            an aligned string.
     * @return the mapping.
     * @throws AnalysisEngineProcessException if there is an error.
     */
    protected abstract Map<Integer, Boolean> createTokenReplaceMap(JCas jcas, AlignedString as)
        throws AnalysisEngineProcessException;

    @Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {
        // Put all SofaChangeAnnotations in a map,
        // where a token position maps to a list of SFCs that should be applied for that token
        Map<Integer, List<SofaChangeAnnotation>> changesMap = createSofaChangesMap(jcas);
        
        // create an AlignedString with all the changes applied and sort by offset
        List<SofaChangeAnnotation> allChanges = new ArrayList<SofaChangeAnnotation>();
        for (Map.Entry<Integer, List<SofaChangeAnnotation>> changesEntry : changesMap.entrySet()) {
            allChanges.addAll(changesEntry.getValue());
        }
        Collections.sort(allChanges, new AnnotationComparator());

        AlignedString as = new AlignedString(jcas.getDocumentText());
        NormalizationUtils.applyChanges(as, allChanges);

        // create a map showing which token should be kept and which should be replaced
        // "true" means replace with changed version
        Map<Integer, Boolean> tokenReplaceMap = createTokenReplaceMap(jcas, as);

        // add SofaChangeAnnotation to indexes if replace is valid
        for (int key : tokenReplaceMap.keySet()) {
            if (tokenReplaceMap.get(key)) {
                for (SofaChangeAnnotation c : changesMap.get(key)) {
                    c.addToIndexes();
                }
            }
        }
    }
}
