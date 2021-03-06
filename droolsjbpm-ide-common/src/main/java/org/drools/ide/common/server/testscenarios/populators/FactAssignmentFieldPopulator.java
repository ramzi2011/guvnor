/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.ide.common.server.testscenarios.populators;

import org.drools.base.TypeResolver;
import org.drools.ide.common.client.modeldriven.testing.FactAssignmentField;
import org.drools.ide.common.client.modeldriven.testing.Field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class FactAssignmentFieldPopulator
        extends FieldPopulator {

    private final Object                     fact;
    private final Collection<FieldPopulator> subFieldPopulators = new ArrayList<FieldPopulator>();
    private final ClassLoader                classLoader;

    public FactAssignmentFieldPopulator(Object factObject,
                                        FactAssignmentField field,
                                        TypeResolver resolver,
                                        ClassLoader classLoader) throws ClassNotFoundException,
                                                                IllegalAccessException,
                                                                InstantiationException {
        super( factObject,
               field.getName() );
        this.fact = resolver.resolveType( resolver.getFullTypeName( field.getFact().getType() ) ).newInstance();
        this.classLoader = classLoader;

        initSubFieldPopulators( field,
                                resolver );
    }

    private void initSubFieldPopulators(FactAssignmentField field,
                                        TypeResolver resolver) throws ClassNotFoundException,
                                                              InstantiationException,
                                                              IllegalAccessException {
        FieldPopulatorFactory fieldPopulatorFactory = new FieldPopulatorFactory( fact,
                                                                                 resolver,
                                                                                 classLoader );
        for ( Field subField : field.getFact().getFieldData() ) {
            try {
                subFieldPopulators.add( fieldPopulatorFactory.getFieldPopulator( subField ) );
            } catch ( IllegalArgumentException e ) {
                // This should never happen, but I don't trust myself or the legacy test scenarios we have.
                // If the field value is null then it is safe to ignore it.
            }
        }
    }

    @Override
    public void populate(Map<String, Object> populatedData) {
        populateField( fact,
                       populatedData );
        for ( FieldPopulator fieldPopulator : subFieldPopulators ) {
            fieldPopulator.populate( populatedData );
        }
    }
}
