package com.confer.imgstoremini.util.PaletteExtraction;

public enum KernelOpenCLENUM {
    COMPUTE_COLOR_DISTANCE("\n" +
            "__kernel void compute_color_distance(__global const uchar4* colors,  // Image pixels\n" +
            "                                     __global const float* query_color,  // Current mean shift point\n" +
            "                                     __global float* distances, \n" +
            "                                     const int num_pixels) {\n" +
            "    int idx = get_global_id(0);\n" +
            "    if (idx < num_pixels) {\n" +
            "        float dist = 0.0f;\n" +
            "        // Compute Euclidean distance between color pixels (in RGB)\n" +
            "        dist += pow(colors[idx].x - query_color[0], 2);  // Red\n" +
            "        dist += pow(colors[idx].y - query_color[1], 2);  // Green\n" +
            "        dist += pow(colors[idx].z - query_color[2], 2);  // Blue\n" +
            "        distances[idx] = sqrt(dist);\n" +
            "    }\n" +
            "}\n"),

    FIND_NEIGHBORS_WITHIN_BANDWIDTH("\n" +
            "__kernel void find_neighbors_within_bandwidth(__global const float* distances,\n" +
            "                                              __global const uchar4* colors,  // Image pixels\n" +
            "                                              __global uchar4* new_mean,  // Updated mean (centroid) of the neighborhood\n" +
            "                                              const int num_pixels, \n" +
            "                                              const float bandwidth) {\n" +
            "    int idx = get_global_id(0);\n" +
            "    if (idx < num_pixels) {\n" +
            "        if (distances[idx] <= bandwidth) {\n" +
            "            // Add the color to the new mean shift point (centroid)\n" +
            "            new_mean[0].x += colors[idx].x;\n" +
            "            new_mean[0].y += colors[idx].y;\n" +
            "            new_mean[0].z += colors[idx].z;\n" +
            "            new_mean[0].w += 1;  // Increment count of points in this neighborhood\n" +
            "        }\n" +
            "    }" +
            "}"),

    UPDATE_CLUSTER_CENTER("\n" +
            "__kernel void update_cluster_center(__global uchar4* new_mean,  // New mean of the cluster\n" +
            "                                    __global float* new_center,  // Output updated color centroid\n" +
            "                                    const int dim) {\n" +
            "    int idx = get_global_id(0);\n" +
            "    if (idx == 0 && new_mean[0].w > 0) {  // At least one point within the bandwidth\n" +
            "        new_center[0] = new_mean[0].x / new_mean[0].w;  // Mean of Red channel\n" +
            "        new_center[1] = new_mean[0].y / new_mean[0].w;  // Mean of Green channel\n" +
            "        new_center[2] = new_mean[0].z / new_mean[0].w;  // Mean of Blue channel\n" +
            "    }\n" +
            "}\n"),

    CHECK_CONVERGENCE("\n" +
            "__kernel void check_convergence(__global const float* old_center, \n" +
            "                                 __global const float* new_center, \n" +
            "                                 __global int* converged_flag,\n" +
            "                                 const float epsilon) {\n" +
            "    int idx = get_global_id(0);\n" +
            "    if (idx == 0) {\n" +
            "        float shift = 0.0f;\n" +
            "        for (int i = 0; i < 3; i++) {  // RGB channels\n" +
            "            shift += pow(new_center[i] - old_center[i], 2);\n" +
            "        }\n" +
            "        shift = sqrt(shift);\n" +
            "        \n" +
            "        if (shift < epsilon) {\n" +
            "            *converged_flag = 1;  // Converged\n" +
            "        } else {\n" +
            "            *converged_flag = 0;  // Not converged\n" +
            "        }\n" +
            "    }\n" +
            "}\n"),

    ASSIGN_PIXEL_TO_CLUSTER("\n" +
            "__kernel void assign_pixel_to_cluster(__global const uchar4* colors, \n" +
            "                                      __global const float* centroids,  // List of final cluster centroids\n" +
            "                                      __global int* cluster_assignments, \n" +
            "                                      const int num_pixels, \n" +
            "                                      const int num_clusters) {\n" +
            "    int idx = get_global_id(0);\n" +
            "    if (idx < num_pixels) {\n" +
            "        float min_dist = FLT_MAX;\n" +
            "        int closest_cluster = -1;\n" +
            "\n" +
            "        // Find the closest centroid for this pixel\n" +
            "        for (int i = 0; i < num_clusters; i++) {\n" +
            "            float dist = 0.0f;\n" +
            "            dist += pow(colors[idx].x - centroids[i * 3], 2);  // Red\n" +
            "            dist += pow(colors[idx].y - centroids[i * 3 + 1], 2);  // Green\n" +
            "            dist += pow(colors[idx].z - centroids[i * 3 + 2], 2);  // Blue\n" +
            "\n" +
            "            dist = sqrt(dist);\n" +
            "            if (dist < min_dist) {\n" +
            "                min_dist = dist;\n" +
            "                closest_cluster = i;\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        cluster_assignments[idx] = closest_cluster;\n" +
            "    }\n" +
            "}\n"),

    COMPUTE_HISTOGRAM("\n" +
            "__kernel void compute_histogram(\n" +
            "    __global const unsigned char* data,\n" +
            "    __global unsigned int* histogram,\n" +
            "    const unsigned int data_size) {\n" +
            "\n" +
            "    // Global thread ID\n" +
            "    int gid = get_global_id(0);\n" +
            "\n" +
            "    // Check bounds\n" +
            "    if (gid >= data_size / 3) return;\n" +
            "\n" +
            "    // Compute histogram index from RGB values\n" +
            "    int r = data[gid * 3];\n" +
            "    int g = data[gid * 3 + 1];\n" +
            "    int b = data[gid * 3 + 2];\n" +
            "    int index = r * 65536 + g * 256 + b;\n" +
            "\n" +
            "    // Increment the histogram bin atomically\n" +
            "    atomic_inc(&histogram[index]);\n" +
            "}\n"),

    KMEANS_UPDATE_CENTROIDS(  "\n" +
            "__kernel void update_centroids(\n" +
            "    __global const float* points,\n" +
            "    __global const int* assignments,\n" +
            "    __global float* centroids,\n" +
            "    __global int* counts,\n" +
            "    const int num_points,\n" +
            "    const int num_centroids) {\n" +
            "    int i = get_global_id(0); // Point index\n" +
            "    if (i >= num_points) return;\n" +
            "    \n" +
            "    // Get the assigned centroid index\n" +
            "    int centroid_idx = assignments[i];\n" +
            "    \n" +
            "    // Use bitwise reinterpretation to safely add float as int\n" +
            "    atomic_add((__global int*)&centroids[centroid_idx * 3], as_int(points[i * 3]));\n" +
            "    atomic_add((__global int*)&centroids[centroid_idx * 3 + 1], as_int(points[i * 3 + 1]));\n" +
            "    atomic_add((__global int*)&centroids[centroid_idx * 3 + 2], as_int(points[i * 3 + 2]));\n" +
            "    \n" +
            "    // Increment the count for this centroid\n" +
            "    atomic_add(&counts[centroid_idx], 1);\n" +
            "}"),

    KMEANS_ASSIGN_CLUSTER( "__kernel void assign_clusters(\n" +
            "    __global const float* points,\n" +
            "    __global const float* centroids,\n" +
            "    __global int* assignments,\n" +
            "    const int num_points,\n" +
            "    const int num_centroids) {\n" +
            "    int i = get_global_id(0); // Point index\n" +
            "    if (i >= num_points) return;\n" +
            "    float min_distance = FLT_MAX;\n" +
            "    int best_centroid = 0;\n" +
            "    for (int j = 0; j < num_centroids; j++) {\n" +
            "        float dx = points[i * 3] - centroids[j * 3];\n" +
            "        float dy = points[i * 3 + 1] - centroids[j * 3 + 1];\n" +
            "        float dz = points[i * 3 + 2] - centroids[j * 3 + 2];\n" +
            "        float distance = dx * dx + dy * dy + dz * dz;\n" +
            "        if (distance < min_distance) {\n" +
            "            min_distance = distance;\n" +
            "            best_centroid = j;\n" +
            "        }\n" +
            "    }\n" +
            "    assignments[i] = best_centroid;\n" +
            "}"),

    SPECTRAL_CLUSTERING_ASSIGN_CLUSTER("__kernel void assign_clusters(\n" +
            "    __global const float* points,\n" +
            "    __global const float* eigenvectors,\n" +
            "    __global int* assignments,\n" +
            "    const int num_points,\n" +
            "    const int num_clusters) {\n" +
            "    int i = get_global_id(0);\n" +
            "    if (i >= num_points) return;\n" +
            "    float min_distance = FLT_MAX;\n" +
            "    int best_cluster = 0;\n" +
            "    for (int j = 0; j < num_clusters; j++) {\n" +
            "        float distance = 0.0f;\n" +
            "        for (int k = 0; k < 3; k++) {\n" +
            "            float dx = points[i * 3 + k] - eigenvectors[j * 3 + k];\n" +
            "            distance += dx * dx;\n" +
            "        }\n" +
            "        if (distance < min_distance) {\n" +
            "            min_distance = distance;\n" +
            "            best_cluster = j;\n" +
            "        }\n" +
            "    }\n" +
            "    assignments[i] = best_cluster;\n" +
            "}"),

    SPECTRAL_CLUSTERING_UPDATE_CLUSTER( "__kernel void update_clusters(\n" +
            "    __global const float* points,\n" +
            "    __global const int* assignments,\n" +
            "    __global float* eigenvectors,\n" +
            "    const int num_points,\n" +
            "    const int num_clusters) {\n" +
            "    int i = get_global_id(0);\n" +
            "    if (i >= num_clusters) return;\n" +
            "    float sum[3] = {0.0f, 0.0f, 0.0f};\n" +
            "    int count = 0;\n" +
            "    for (int j = 0; j < num_points; j++) {\n" +
            "        if (assignments[j] == i) {\n" +
            "            sum[0] += points[j * 3];\n" +
            "            sum[1] += points[j * 3 + 1];\n" +
            "            sum[2] += points[j * 3 + 2];\n" +
            "            count++;\n" +
            "        }\n" +
            "    }\n" +
            "    if (count > 0) {\n" +
            "        eigenvectors[i * 3] = sum[0] / count;\n" +
            "        eigenvectors[i * 3 + 1] = sum[1] / count;\n" +
            "        eigenvectors[i * 3 + 2] = sum[2] / count;\n" +
            "    }\n" +
            "}"),

    GMM_E_STEP("\n" +
            "__kernel void e_step(\n" +
            "    __global const float* points,\n" +
            "    __global const float* means,\n" +
            "    __global const float* covariances,\n" +
            "    __global const float* priors,\n" +
            "    __global float* responsibilities,\n" +
            "    const int num_points,\n" +
            "    const int num_clusters) {\n" +
            "    int i = get_global_id(0);\n" +
            "    if (i >= num_points) return;\n" +
            "    float max_prob = -FLT_MAX;\n" +
            "    int best_cluster = 0;\n" +
            "    for (int j = 0; j < num_clusters; j++) {\n" +
            "        float prob = 0.0f;\n" +
            "        for (int k = 0; k < 3; k++) {\n" +
            "            float diff = points[i * 3 + k] - means[j * 3 + k];\n" +
            "            prob += diff * diff / covariances[j * 3 + k];\n" +  // Correctly handling diagonal covariance
            "        }\n" +
            "        prob = -0.5f * prob - 0.5f * log(2.0f * M_PI) - 0.5f * log(covariances[j * 3]);\n" +  // Use log to avoid underflow
            "        prob += log(priors[j]);\n" +  // Incorporate prior
            "        if (prob > max_prob) {\n" +
            "            max_prob = prob;\n" +
            "            best_cluster = j;\n" +
            "        }\n" +
            "    }\n" +
            "    responsibilities[i * num_clusters + best_cluster] = 1.0f;\n" +  // Assign highest responsibility
            "}"),

    GMM_M_STEP("\n" +
            "__kernel void m_step(\n" +
            "    __global const float* points,\n" +
            "    __global const float* responsibilities,\n" +
            "    __global float* means,\n" +
            "    __global float* covariances,\n" +
            "    __global float* priors,\n" +
            "    const int num_points,\n" +
            "    const int num_clusters) {\n" +
            "    int j = get_global_id(0);\n" +
            "    if (j >= num_clusters) return;\n" +
            "    float sum[3] = {0.0f, 0.0f, 0.0f};\n" +
            "    float total_responsibility = 0.0f;\n" +
            "    for (int i = 0; i < num_points; i++) {\n" +
            "        float resp = responsibilities[i * num_clusters + j];\n" +
            "        total_responsibility += resp;\n" +
            "        for (int k = 0; k < 3; k++) {\n" +
            "            sum[k] += resp * points[i * 3 + k];\n" +
            "        }\n" +
            "    }\n" +
            "    for (int k = 0; k < 3; k++) {\n" +
            "        means[j * 3 + k] = sum[k] / total_responsibility;\n" +
            "    }\n" +
            "    priors[j] = total_responsibility / num_points;\n" +
            "    // Update covariance (diagonal) here\n" +
            "    for (int k = 0; k < 3; k++) {\n" +
            "        float sum_diff_square = 0.0f;\n" +
            "        for (int i = 0; i < num_points; i++) {\n" +
            "            float diff = points[i * 3 + k] - means[j * 3 + k];\n" +
            "            sum_diff_square += responsibilities[i * num_clusters + j] * diff * diff;\n" +
            "        }\n" +
            "        covariances[j * 3 + k] = sum_diff_square / total_responsibility;\n" +
            "    }\n" +
            "}");

    private final String kernelCode;
    KernelOpenCLENUM(String kernelCode){
        this.kernelCode = kernelCode;
    }
    public String getKernelCode(){return this.kernelCode;}
}
