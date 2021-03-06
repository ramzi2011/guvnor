/*
 * Copyright 2005 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.drools.guvnor.client.asseteditor;

import org.drools.guvnor.client.common.GenericCallback;
import org.drools.guvnor.client.common.LoadingPopup;
import org.drools.guvnor.client.explorer.ClientFactory;
import org.drools.guvnor.client.messages.ConstantsCore;
import org.drools.guvnor.client.moduleeditor.ArtifactEditor;
import org.drools.guvnor.client.rpc.Asset;
import org.drools.guvnor.client.rpc.RepositoryServiceFactory;
import org.drools.guvnor.client.widgets.MessageWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * The main layout parent/controller the rule viewer.
 */
public class RuleViewerWrapper extends GuvnorEditor
    implements
    RefreshAssetEditorEvent.Handler,
    ShowMessageEvent.Handler {
    private ConstantsCore constants            = GWT.create( ConstantsCore.class );

    private Asset               asset;
    private boolean             isHistoricalReadOnly = false;
    private RuleViewerSettings  ruleViewerSettings   = null;

    private final MessageWidget messageWidget        = new MessageWidget();

    VerticalPanel               layout               = new VerticalPanel();
    private final ClientFactory clientFactory;
    private final EventBus      eventBus;

    public RuleViewerWrapper(ClientFactory clientFactory,
                             EventBus eventBus,
                             Asset asset) {
        this( clientFactory,
                eventBus,
                asset,
                false );
    }

    public RuleViewerWrapper(ClientFactory clientFactory,
                             EventBus eventBus,
                             final Asset asset,
                             boolean isHistoricalReadOnly) {
        this.clientFactory = clientFactory;
        this.eventBus = eventBus;
        this.asset = asset;
        this.isHistoricalReadOnly = isHistoricalReadOnly;

        //Wire-up event handlers
        eventBus.addHandler( RefreshAssetEditorEvent.TYPE,
                             this );
        eventBus.addHandler( ShowMessageEvent.TYPE,
                             this );

        initWidget( layout );
        setWidth( "100%" );
        render();
    }

    private void render() {
        ArtifactEditor artifactEditor = new ArtifactEditor(
                                                            asset,
                                                            clientFactory,
                                                            eventBus,
                                                            this.isHistoricalReadOnly );

        RuleViewer ruleViewer = new RuleViewer(
                                                asset,
                                                clientFactory,
                                                eventBus,
                                                ruleViewerSettings );

        boolean readOnly = isHistoricalReadOnly || asset.isReadonly() || asset.isArchived() || (this.ruleViewerSettings != null && this.ruleViewerSettings.isStandalone());
        Widget actionToolBar = clientFactory.getPerspectiveFactory().getAssetEditorActionToolbar( "author",
                                                                                                  asset,
                                                                                                  ruleViewer.getAssetEditor(),
                                                                                                  clientFactory,
                                                                                                  eventBus,
                                                                                                  readOnly );

        layout.clear();
        layout.add( actionToolBar );
        layout.add( messageWidget );

        TabPanel tabPanel = new TabPanel();
        tabPanel.setWidth( "100%" );

        ScrollPanel scrollPanel = new ScrollPanel();
        scrollPanel.add( artifactEditor );
        tabPanel.add( scrollPanel,
                      constants.Attributes() );

        scrollPanel = new ScrollPanel();
        scrollPanel.add( ruleViewer );
        tabPanel.add( scrollPanel,
                      constants.Edit() );
        tabPanel.selectTab( 1 );

        layout.add( tabPanel );
    }

    public void onRefreshAsset(RefreshAssetEditorEvent refreshAssetEditorEvent) {
        //AssetUUID == null means to refresh all asset editors contained by the specified module. 
        if ((refreshAssetEditorEvent.getAssetUUID() == null && asset.getMetaData().getModuleName().equals(refreshAssetEditorEvent.getModuleName())) || asset.getUuid().equals( refreshAssetEditorEvent.getAssetUUID() ) ) {
            LoadingPopup.showMessage( constants.RefreshingItem() );
            RepositoryServiceFactory.getAssetService().loadRuleAsset( asset.getUuid(),
                                                                      new GenericCallback<Asset>() {
                                                                          public void onSuccess(Asset a) {
                                                                              asset = a;
                                                                              render();
                                                                              LoadingPopup.close();
                                                                          }
                                                                      } );
        }
    }

    public void onShowMessage(ShowMessageEvent event) {
        messageWidget.showMessage( event.getMessage(),
                                   event.getMessageType() );
    }

}
