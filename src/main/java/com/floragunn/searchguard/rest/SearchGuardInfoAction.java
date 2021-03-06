/*
 * Copyright 2015 floragunn UG (haftungsbeschränkt)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.floragunn.searchguard.rest;

import static org.elasticsearch.rest.RestRequest.Method.GET;

import java.security.cert.X509Certificate;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Provider;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

import com.floragunn.searchguard.configuration.PrivilegesEvaluator;
import com.floragunn.searchguard.support.ConfigConstants;
import com.floragunn.searchguard.user.User;

public class SearchGuardInfoAction extends BaseRestHandler {

    private final Provider<PrivilegesEvaluator> evaluator;
    
    @Inject
    public SearchGuardInfoAction(final Settings settings, final RestController controller, final Client client, Provider<PrivilegesEvaluator> evaluator) {
        super(settings, controller, client);
        this.evaluator = evaluator;
        controller.registerHandler(GET, "/_searchguard/authinfo", this);
    }

    @Override
    protected void handleRequest(final RestRequest request, final RestChannel channel, final Client client) throws Exception {

        BytesRestResponse response = null;
        final XContentBuilder builder = channel.newBuilder();

        try {

            final X509Certificate[] certs = request.getFromContext(ConfigConstants.SG_SSL_PEER_CERTIFICATES);
            builder.startObject();

            builder.field("user", request.getFromContext(ConfigConstants.SG_USER));
            builder.field("remote_address", request.getFromContext(ConfigConstants.SG_REMOTE_ADDRESS));
            builder.field("sg_roles", evaluator.get().mapSgRoles((User) request.getFromContext(ConfigConstants.SG_USER), (TransportAddress) request.getFromContext(ConfigConstants.SG_REMOTE_ADDRESS)));
            builder.field("principal", request.getFromContext(ConfigConstants.SG_SSL_PRINCIPAL));
            builder.field("peer_certificates", certs != null && certs.length > 0 ? certs.length + "" : "0");
            //builder.field("_debug_request", LogHelper.toString(request));
            builder.endObject();

            response = new BytesRestResponse(RestStatus.OK, builder);
        } catch (final Exception e1) {
            builder.startObject();
            builder.field("error", e1.toString());
            builder.endObject();
            response = new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, builder);
        }

        channel.sendResponse(response);
    }
}
